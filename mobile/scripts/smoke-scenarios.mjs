import { createServer } from "vite";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");

function installLocalStorage() {
  const store = new Map();
  Object.defineProperty(globalThis, "localStorage", {
    configurable: true,
    value: {
      get length() {
        return store.size;
      },
      clear() {
        store.clear();
      },
      getItem(key) {
        return store.has(String(key)) ? store.get(String(key)) : null;
      },
      key(index) {
        return Array.from(store.keys())[index] ?? null;
      },
      removeItem(key) {
        store.delete(String(key));
      },
      setItem(key, value) {
        store.set(String(key), String(value));
      },
    },
  });
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

async function scenario(name, run) {
  process.stdout.write(`- ${name}... `);
  await run();
  console.log("ok");
}

function currentPlayer(state) {
  return state.players[state.turnOrder.currentPlayerIndex ?? 0] ?? state.players[0];
}

function workerMatchesSlot(worker, slot) {
  if (slot.requiredQualification === "UNSKILLED") return true;
  if (worker.qualificationType !== "SKILLED") return false;
  return !slot.requiredSector || worker.sector === slot.requiredSector;
}

function firstAssignmentTarget(state) {
  const actor = currentPlayer(state);
  const worker = state.workers.find((candidate) => candidate.classType === actor.classType && !candidate.tiedContract);
  assert(worker, "expected an assignable worker for the current class");

  for (const enterprise of state.enterprises) {
    const slot = enterprise.slots.find((candidate) => !candidate.occupiedWorkerId && workerMatchesSlot(worker, candidate));
    if (slot) {
      return { actor, worker, enterprise, slot };
    }
  }

  throw new Error("expected a compatible enterprise slot");
}

installLocalStorage();

const server = await createServer({
  root,
  configFile: resolve(root, "vite.config.ts"),
  logLevel: "error",
  server: { middlewareMode: true },
  appType: "custom",
});

try {
  const { createLocalGameApi } = await server.ssrLoadModule("/src/lib/local-game-engine.ts");
  const api = createLocalGameApi();

  await scenario("reset and fetch initial game", async () => {
    const response = await api.resetGame();
    assert(response.gameState.currentPhase === "ACTIONS", "expected ACTIONS phase after reset");
    assert(response.legalMoves.length > 0, "expected generated legal moves");
  });

  await scenario("apply two-player setup with a bot opponent", async () => {
    const response = await api.setupGame({
      playerCount: 2,
      controlModes: {
        WORKER: "HUMAN",
        CAPITALIST: "BOT",
      },
      botStrategyModes: {
        WORKER: "HEURISTIC_FALLBACK",
        CAPITALIST: "CARD_DRIVEN_SIMPLE_AUTOMA",
      },
    });
    assert(response.gameState.players.length === 2, "expected two active players");
    assert(currentPlayer(response.gameState).classType === "WORKER", "expected worker to start");
  });

  let assignedWorkerId = "";

  await scenario("preview and submit visual worker assignment payload", async () => {
    const response = await api.getGame();
    const assignmentMove = response.legalMoves.find((move) => move.actionType === "ASSIGN_WORKERS");
    assert(assignmentMove, "expected ASSIGN_WORKERS to be legal");

    const { actor, worker, enterprise, slot } = firstAssignmentTarget(response.gameState);
    assignedWorkerId = worker.id;
    const parameters = {
      actorPlayerId: actor.playerId,
      assignments: [{
        workerId: worker.id,
        targetType: "ENTERPRISE_SLOT",
        targetId: `${enterprise.id}:${slot.id}`,
      }],
    };

    const preview = await api.previewAction({ actionType: "ASSIGN_WORKERS", parameters });
    assert(preview.accepted, `expected assignment preview to be accepted: ${preview.errors.join(", ")}`);

    const command = await api.submitCommand({ actionType: "ASSIGN_WORKERS", parameters });
    assert(command.accepted, `expected assignment command to be accepted: ${command.errors.join(", ")}`);
    const updatedWorker = command.gameState.workers.find((candidate) => candidate.id === assignedWorkerId);
    assert(updatedWorker?.location === "ENTERPRISE_SLOT", "expected worker to move to an enterprise slot");
  });

  await scenario("end human turn and let bot return to a human decision", async () => {
    const before = await api.getGame();
    const actor = currentPlayer(before.gameState);
    const end = await api.submitCommand({ actionType: "END_TURN", parameters: { actorPlayerId: actor.playerId } });
    assert(end.accepted, "expected END_TURN to be accepted");
    assert(currentPlayer(end.gameState).controlMode === "BOT", "expected bot player after ending worker turn");

    const bot = await api.playBotUntilHuman();
    assert(bot.executedSteps > 0, "expected at least one bot step");
    assert(bot.stoppedAtHumanDecisionPoint, "expected bot autoplay to stop at a human");
    assert(currentPlayer(bot.gameState).controlMode === "HUMAN", "expected current player to be human");
  });

  await scenario("save and load local state", async () => {
    const saved = await api.saveGame("offline-smoke");
    assert(saved.filePath === "localStorage:offline-smoke", "expected localStorage save path");

    const loaded = await api.loadGame("offline-smoke");
    const worker = loaded.gameState.workers.find((candidate) => candidate.id === assignedWorkerId);
    assert(worker?.location === "ENTERPRISE_SLOT", "expected loaded state to keep worker assignment");
  });

  await scenario("round preparation adds migration workers", async () => {
    await api.resetGame();
    const before = await api.getGame();
    const actor = currentPlayer(before.gameState);
    const workerCountBefore = before.gameState.workers.filter((worker) => worker.classType === "WORKER").length;
    const result = await api.submitCommand({ actionType: "ADVANCE_TO_NEXT_ROUND", parameters: { actorPlayerId: actor.playerId } });
    assert(result.accepted, `expected next round to be accepted: ${result.errors.join(", ")}`);
    const workerCountAfter = result.gameState.workers.filter((worker) => worker.classType === "WORKER").length;
    assert(workerCountAfter > workerCountBefore, "expected migration to add a worker at round preparation");
    assert(result.gameState.eventLog.some((event) => event.type === "MIGRATION_RESOLVED"), "expected migration event");
  });

  await scenario("final reset", async () => {
    const response = await api.resetGame();
    assert(response.gameState.currentRound === 1, "expected reset to return to round 1");
  });

  console.log("Offline smoke scenarios completed.");
} finally {
  await server.close();
}
