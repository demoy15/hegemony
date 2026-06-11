import { useMemo } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { GameTableScreen } from "@/features/board/components/game-table-screen";
import { gameApi } from "@/lib/api";
import { useUiStore } from "@/store/use-ui-store";
import type { ActionType } from "@/types/game";

export function App() {
  const queryClient = useQueryClient();
  const setLastCommandResult = useUiStore((s) => s.setLastCommandResult);
  const setLastPreviewResult = useUiStore((s) => s.setLastPreviewResult);
  const lastCommandResult = useUiStore((s) => s.lastCommandResult);
  const lastPreviewResult = useUiStore((s) => s.lastPreviewResult);
  const saveFileName = useUiStore((s) => s.saveFileName);
  const setSaveFileName = useUiStore((s) => s.setSaveFileName);

  const gameQuery = useQuery({
    queryKey: ["game"],
    queryFn: gameApi.getGame,
    refetchInterval: 4000,
  });

  const legalMovesQuery = useQuery({
    queryKey: ["legalMoves"],
    queryFn: gameApi.getLegalMoves,
    refetchInterval: 4000,
  });

  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ["game"] });
    await queryClient.invalidateQueries({ queryKey: ["legalMoves"] });
  };

  const previewMutation = useMutation({
    mutationFn: ({
      actionType,
      parameters,
      optionalModifier,
      optionalCardReference,
    }: {
      actionType: ActionType;
      parameters: Record<string, unknown>;
      optionalModifier?: string;
      optionalCardReference?: string;
    }) => gameApi.previewAction({ actionType, parameters, optionalModifier, optionalCardReference }),
    onSuccess: (result) => setLastPreviewResult(result),
  });

  const playBotTurnMutation = useMutation({
    mutationFn: gameApi.playBotTurn,
    onSuccess: async () => {
      await refresh();
    },
  });

  const playUntilHumanMutation = useMutation({
    mutationFn: gameApi.playBotUntilHuman,
    onSuccess: async () => {
      await refresh();
    },
  });

  const commandMutation = useMutation({
    mutationFn: ({ actionType, parameters }: { actionType: ActionType; parameters: Record<string, unknown> }) =>
      gameApi.submitCommand({ actionType, parameters }),
    onSuccess: async (result, variables) => {
      setLastCommandResult(result);

      const nextState = result.gameState;
      const nextPlayerIndex = Number.isInteger(nextState.turnOrder?.currentPlayerIndex)
        ? Number(nextState.turnOrder.currentPlayerIndex)
        : 0;
      const nextActor = Array.isArray(nextState.players) ? nextState.players[nextPlayerIndex] : undefined;

      if (result.accepted && variables.actionType === "END_TURN" && nextActor?.controlMode === "BOT") {
        await playUntilHumanMutation.mutateAsync();
        return;
      }

      await refresh();
    },
  });

  const setupMutation = useMutation({
    mutationFn: gameApi.setupGame,
    onSuccess: async () => {
      setLastCommandResult(undefined);
      setLastPreviewResult(undefined);
      await refresh();
    },
  });

  const saveMutation = useMutation({
    mutationFn: () => gameApi.saveGame(saveFileName),
  });

  const loadMutation = useMutation({
    mutationFn: () => gameApi.loadGame(saveFileName),
    onSuccess: async () => {
      await refresh();
    },
  });

  const resetMutation = useMutation({
    mutationFn: gameApi.resetGame,
    onSuccess: async () => {
      setLastCommandResult(undefined);
      setLastPreviewResult(undefined);
      await refresh();
    },
  });

  const state = gameQuery.data?.gameState;
  const composerMetadata = gameQuery.data?.composerMetadata;
  const legalMoves = useMemo(() => legalMovesQuery.data?.legalMoves ?? [], [legalMovesQuery.data]);

  if (gameQuery.isError || legalMovesQuery.isError) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background px-6 text-foreground">
        <div className="max-w-2xl rounded-xl border border-red-500/40 bg-red-500/10 p-5 text-sm">
          <p className="mb-2 font-semibold">Frontend failed to load game data</p>
          <p className="text-red-100">
            {String(gameQuery.error ?? legalMovesQuery.error ?? "Unknown error")}
          </p>
        </div>
      </div>
    );
  }

  if (gameQuery.isLoading || !state || !composerMetadata) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background px-6 text-foreground">
        <div className="w-full max-w-xl rounded-xl border border-zinc-700/70 bg-zinc-900/75 p-5 text-center">
          <p className="text-base font-semibold text-zinc-100">Loading game state...</p>
          <p className="mt-2 text-sm text-zinc-300">
            Waiting for backend response from <code>/api/game</code> and <code>/api/game/legal-moves</code>.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[radial-gradient(circle_at_18%_8%,rgba(180,132,56,0.14),transparent_28%),radial-gradient(circle_at_82%_12%,rgba(20,106,113,0.16),transparent_30%),linear-gradient(180deg,#090c0c_0%,#111716_48%,#090b0b_100%)] text-foreground">
      <div className="mx-auto w-full max-w-[2440px] space-y-4 px-3 py-3 sm:px-5 lg:px-6 2xl:px-8">
        <GameTableScreen
          state={state}
          legalMoves={legalMoves}
          composerMetadata={composerMetadata}
          isPreviewing={previewMutation.isPending}
          isSubmitting={commandMutation.isPending}
          lastPreviewResult={lastPreviewResult}
          lastCommandResult={lastCommandResult}
          lastBotSummary={state.lastBotTurnSummary}
          isBotTurnLoading={playBotTurnMutation.isPending}
          isBotUntilLoading={playUntilHumanMutation.isPending}
          saveFileName={saveFileName}
          setSaveFileName={setSaveFileName}
          isSaving={saveMutation.isPending}
          isLoading={loadMutation.isPending}
          isResetting={resetMutation.isPending}
          isApplyingSetup={setupMutation.isPending}
          onPreview={(actionType, parameters) => previewMutation.mutate({ actionType, parameters })}
          onSubmit={(actionType, parameters) => commandMutation.mutate({ actionType, parameters })}
          onPlayBotTurn={() => playBotTurnMutation.mutate()}
          onPlayBotUntilHuman={() => playUntilHumanMutation.mutate()}
          onApplySetup={(payload) => setupMutation.mutate(payload)}
          onSave={() => saveMutation.mutate()}
          onLoad={() => loadMutation.mutate()}
          onReset={() => resetMutation.mutate()}
        />
      </div>
    </div>
  );
}
