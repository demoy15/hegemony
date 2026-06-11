import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Bot,
  Check,
  ChevronDown,
  ClipboardList,
  Eye,
  LayoutDashboard,
  ListChecks,
  Loader2,
  Play,
  RefreshCw,
  RotateCcw,
  Save,
  Settings,
  Upload,
  Wifi,
  X,
} from "lucide-react";
import { twMerge } from "tailwind-merge";
import { createGameApi, type SetupPayload } from "@/lib/api";
import {
  actorPlayerId,
  currentPlayer,
  enterpriseMarket,
  purchaseRowsFromMaps,
  seedParameters,
  supplierOptions,
  toInt,
  toRecord,
  toString,
  workerLabel,
} from "@/lib/action-parameters";
import {
  ACTION_LABEL,
  CLASS_LABEL,
  CLASS_SHORT,
  POLICY_LABEL,
  classTone,
  compactActionSummary,
  phaseLabel,
  resourceLabel,
} from "@/lib/labels";
import type {
  ActionType,
  BotTurnSummary,
  ClassType,
  CommandResponse,
  ComposerMetadata,
  GameState,
  LegalMove,
  PlayerControlMode,
  PreviewActionResponse,
} from "@/types/game";

type TabKey = "state" | "actions" | "bot" | "log" | "setup";

const PLAYER_CLASS_ORDER_BY_COUNT: Record<number, ClassType[]> = {
  2: ["WORKER", "CAPITALIST"],
  3: ["WORKER", "CAPITALIST", "MIDDLE_CLASS"],
  4: ["WORKER", "CAPITALIST", "MIDDLE_CLASS", "STATE"],
};

const CONSUME_ACTIONS: ActionType[] = ["CONSUME_LUXURY", "CONSUME_EDUCATION", "CONSUME_HEALTHCARE"];

type ActionGroupId = "workers" | "policy" | "goods" | "vote" | "flow" | "other";

const ACTION_GROUPS: Array<{ id: ActionGroupId; label: string }> = [
  { id: "workers", label: "Рабочие" },
  { id: "policy", label: "Политика" },
  { id: "goods", label: "Товары" },
  { id: "vote", label: "Голосование" },
  { id: "flow", label: "Ход" },
  { id: "other", label: "Другое" },
];

function actionGroupId(actionType: ActionType): ActionGroupId {
  if (["ASSIGN_WORKERS", "HIRE_WORKER", "PLACE_STRIKES", "PLACE_DEMONSTRATION"].includes(actionType)) {
    return "workers";
  }
  if (["PROPOSE_BILL", "CALL_EXTRAORDINARY_VOTE", "ADJUST_POLICY", "ADVANCE_TO_VOTING"].includes(actionType)) {
    return "policy";
  }
  if (["BUY_GOODS_AND_SERVICES", "CONSUME_HEALTHCARE", "CONSUME_EDUCATION", "CONSUME_LUXURY", "PRODUCE_GOODS", "SELL_GOODS", "BUILD_ENTERPRISE"].includes(actionType)) {
    return "goods";
  }
  if (["DECLARE_VOTE_STANCE", "DRAW_VOTING_CUBES", "COMMIT_VOTE_INFLUENCE"].includes(actionType)) {
    return "vote";
  }
  if (["END_TURN", "ADVANCE_GAME_FLOW", "ADVANCE_TO_PRODUCTION", "RESOLVE_PRODUCTION_PHASE", "ADVANCE_TO_SCORING", "RESOLVE_SCORING_PHASE", "ADVANCE_TO_NEXT_ROUND", "RESOLVE_PREPARATION_PHASE", "ADVANCE_ROUND", "START_TURN"].includes(actionType)) {
    return "flow";
  }
  return "other";
}

function cn(...classes: Array<string | false | undefined>) {
  return twMerge(classes.filter(Boolean).join(" "));
}

function Button({
  children,
  className,
  variant = "primary",
  disabled,
  type,
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: "primary" | "secondary" | "outline" | "danger" }) {
  return (
    <button
      {...props}
      type={type ?? "button"}
      disabled={disabled}
      className={cn(
        "inline-flex min-h-11 items-center justify-center gap-2 rounded-md px-3 py-2 text-sm font-semibold transition active:scale-[0.98] disabled:pointer-events-none disabled:opacity-45",
        variant === "primary" && "bg-primary text-primary-foreground shadow-[0_12px_26px_-18px_rgba(67,211,167,0.8)]",
        variant === "secondary" && "border border-amber-400/40 bg-amber-400/14 text-amber-50",
        variant === "outline" && "border border-zinc-600/80 bg-black/20 text-zinc-100",
        variant === "danger" && "border border-rose-500/50 bg-rose-500/15 text-rose-100",
        className,
      )}
    >
      {children}
    </button>
  );
}

function Card({ children, className }: { children: React.ReactNode; className?: string }) {
  return (
    <section className={cn("rounded-md border border-zinc-700/70 bg-card/92 p-3 shadow-[0_18px_34px_-28px_rgba(0,0,0,0.9)]", className)}>
      {children}
    </section>
  );
}

function FieldLabel({ children }: { children: React.ReactNode }) {
  return <label className="text-[11px] font-semibold uppercase tracking-[0.14em] text-muted-foreground">{children}</label>;
}

function TextInput(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      {...props}
      className={cn(
        "min-h-11 w-full rounded-md border border-input bg-black/25 px-3 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary",
        props.className,
      )}
    />
  );
}

function SelectInput(props: React.SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select
      {...props}
      className={cn(
        "min-h-11 w-full rounded-md border border-input bg-[#151a18] px-3 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary",
        props.className,
      )}
    />
  );
}

function Badge({ children, tone = "neutral" }: { children: React.ReactNode; tone?: "neutral" | "good" | "warn" | "danger" }) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded px-2 py-1 text-[11px] font-semibold uppercase tracking-wide",
        tone === "neutral" && "border border-zinc-600/70 bg-zinc-800/70 text-zinc-200",
        tone === "good" && "border border-emerald-500/50 bg-emerald-500/15 text-emerald-100",
        tone === "warn" && "border border-amber-400/55 bg-amber-400/15 text-amber-100",
        tone === "danger" && "border border-rose-500/55 bg-rose-500/15 text-rose-100",
      )}
    >
      {children}
    </span>
  );
}

function SectionTitle({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <div className="mb-2">
      <h2 className="text-sm font-semibold uppercase tracking-[0.16em] text-zinc-100">{title}</h2>
      {subtitle && <p className="mt-1 text-xs leading-5 text-muted-foreground">{subtitle}</p>}
    </div>
  );
}

function SpinnerLabel({ label }: { label: string }) {
  return (
    <span className="inline-flex items-center gap-2">
      <Loader2 className="h-4 w-4 animate-spin" />
      {label}
    </span>
  );
}

function stringifyUnknown(value: unknown): string {
  if (value === null || value === undefined) {
    return "";
  }
  if (typeof value === "string") {
    return value;
  }
  try {
    return JSON.stringify(value);
  } catch {
    return String(value);
  }
}

function summarizeRecord(record: Record<string, number> | undefined, fallback = "нет"): string {
  const entries = Object.entries(record ?? {}).filter(([, amount]) => Number(amount) !== 0);
  if (entries.length === 0) {
    return fallback;
  }
  return entries.map(([key, amount]) => `${resourceLabel(key)} ${amount}`).join(", ");
}

function enterpriseStatusLabel(enterprise: GameState["enterprises"][number]): string {
  if (enterprise.functioning) return "работает";
  if (enterprise.fullyEmpty) return "пустое";
  if (enterprise.partiallyFilled) return "не работает";
  return "не работает";
}

function AppTopBar({
  state,
}: {
  state?: GameState;
}) {
  const actor = state ? currentPlayer(state) : undefined;
  const actionsPerPlayer = toInt(state?.turnOrder?.actionsPerPlayer, 5);
  const actionsTaken = Array.isArray(state?.turnOrder?.actionsTakenByPlayer)
    ? state?.turnOrder.actionsTakenByPlayer?.[state.turnOrder.currentPlayerIndex] ?? 0
    : 0;

  return (
    <header className="safe-top sticky top-0 z-30 border-b border-zinc-700/70 bg-[#101412]/96 px-3 pb-3 backdrop-blur">
      <div className="flex items-center justify-between gap-3">
        <div className="min-w-0">
          <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-amber-200">Hegemony Assistant</p>
          <h1 className="truncate text-lg font-semibold text-zinc-50">
            {state ? `Раунд ${state.currentRound}/${state.maxRounds} · ${phaseLabel(state.currentPhase)}` : "Мобильный клиент"}
          </h1>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <Badge tone="good"><Wifi className="h-3.5 w-3.5" /></Badge>
        </div>
      </div>
      <div className="mt-3 grid grid-cols-3 gap-2">
        <div className="rounded-md border border-zinc-700/70 bg-black/20 px-3 py-2">
          <p className="text-[10px] uppercase text-muted-foreground">Ходит</p>
          <p className="truncate text-sm font-semibold">{actor ? CLASS_SHORT[actor.classType] : "н/д"}</p>
        </div>
        <div className="rounded-md border border-zinc-700/70 bg-black/20 px-3 py-2">
          <p className="text-[10px] uppercase text-muted-foreground">Действие</p>
          <p className="text-sm font-semibold">{state?.currentPhase === "ACTIONS" ? `${Math.min(actionsPerPlayer, actionsTaken + 1)}/${actionsPerPlayer}` : "-"}</p>
        </div>
        <div className="rounded-md border border-zinc-700/70 bg-black/20 px-3 py-2">
          <p className="text-[10px] uppercase text-muted-foreground">Режим</p>
          <p className="truncate text-sm font-semibold">Офлайн</p>
        </div>
      </div>
    </header>
  );
}

function BottomNav({ activeTab, setActiveTab }: { activeTab: TabKey; setActiveTab: (tab: TabKey) => void }) {
  const items: Array<{ key: TabKey; label: string; icon: typeof LayoutDashboard }> = [
    { key: "state", label: "Стол", icon: LayoutDashboard },
    { key: "actions", label: "Ход", icon: ListChecks },
    { key: "bot", label: "Бот", icon: Bot },
    { key: "log", label: "Лог", icon: ClipboardList },
    { key: "setup", label: "Опции", icon: Settings },
  ];
  return (
    <nav className="safe-bottom fixed inset-x-0 bottom-0 z-30 border-t border-zinc-700/70 bg-[#101412]/96 px-2 pt-2 backdrop-blur">
      <div className="grid grid-cols-5 gap-1">
        {items.map((item) => {
          const Icon = item.icon;
          const active = activeTab === item.key;
          return (
            <button
              key={item.key}
              type="button"
              onClick={() => setActiveTab(item.key)}
              className={cn(
                "flex min-h-14 flex-col items-center justify-center gap-1 rounded-md px-1 text-[11px] font-semibold transition",
                active ? "bg-primary text-primary-foreground" : "text-zinc-400 hover:bg-zinc-800/80 hover:text-zinc-100",
              )}
            >
              <Icon className="h-5 w-5" />
              <span>{item.label}</span>
            </button>
          );
        })}
      </div>
    </nav>
  );
}

function StateScreen({ state }: { state: GameState }) {
  const actor = currentPlayer(state);
  const vote = state.currentVoteState;
  const visibleBusinessDeals = state.businessDealDeck.visibleCardIds
    .map((id) => state.businessDealCards.find((deal) => deal.id === id))
    .filter((deal): deal is NonNullable<typeof deal> => Boolean(deal));

  return (
    <div className="space-y-3">
      <Card>
        <SectionTitle title="Сводка" />
        <div className="grid grid-cols-2 gap-2">
          <Metric label="Текущий класс" value={actor ? CLASS_LABEL[actor.classType] : "н/д"} />
          <Metric label="Казна" value={String(state.treasury)} />
          <Metric label="Налог" value={`x${state.taxMultiplier}`} />
          <Metric label="Статус" value={state.gameOver ? "Финал" : state.gameStatus} />
        </div>
      </Card>

      <Card>
        <SectionTitle title="Игроки" />
        <div className="space-y-2">
          {state.players.map((player) => (
            <div key={player.playerId} className={cn("rounded-md border p-3", classTone(player.classType))}>
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold">{CLASS_LABEL[player.classType]}</p>
                  <p className="text-xs text-muted-foreground">{player.controlMode} · {player.playerId}</p>
                </div>
                <Badge tone={actor?.playerId === player.playerId ? "good" : "neutral"}>{player.victoryPoints} ПО</Badge>
              </div>
              <div className="mt-3 grid grid-cols-4 gap-2 text-center">
                <Metric label="Деньги" value={String(player.money)} compact />
                <Metric label="Влияние" value={String(player.influence)} compact />
                <Metric label="Поп." value={String(player.population)} compact />
                <Metric label="Благо" value={String(player.welfare)} compact />
              </div>
              <p className="mt-2 text-xs leading-5 text-zinc-300">Ресурсы: {summarizeRecord(player.producedResourceStorage)}</p>
            </div>
          ))}
        </div>
      </Card>

      <Card>
        <SectionTitle title="Политики" />
        <div className="grid gap-2">
          {state.policies.map((policy) => (
            <div key={policy.id} className="grid grid-cols-[1fr,3rem] items-center gap-2 rounded-md border border-zinc-700/70 bg-black/20 p-3">
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold">{POLICY_LABEL[policy.id]}</p>
                <p className="text-xs text-muted-foreground">
                  {policy.occupyingProposalToken ? `Законопроект -> ${policy.occupyingProposalToken.targetCourse ?? "?"}` : "без законопроекта"}
                </p>
              </div>
              <div className="rounded-md border border-amber-400/45 bg-amber-400/15 py-2 text-center text-lg font-bold text-amber-100">
                {policy.currentCourse}
              </div>
            </div>
          ))}
        </div>
      </Card>

      {vote && (
        <Card>
          <SectionTitle title="Голосование" />
          <div className="grid gap-2">
            <Metric label="Политика" value={POLICY_LABEL[vote.activeProposalPolicyId]} />
            <Metric label="Этап" value={vote.votingStage} />
            <Metric label="Счет" value={`${vote.totalForVotes} за / ${vote.totalAgainstVotes} против`} />
          </div>
        </Card>
      )}

      <DetailsCard title="Предприятия" count={state.enterprises.length}>
        <div className="space-y-2">
          {state.enterprises.map((enterprise) => (
            <div key={enterprise.id} className="rounded-md border border-zinc-700/70 bg-black/20 p-3">
              <p className="text-sm font-semibold">{enterprise.name ?? enterprise.id}</p>
              <p className="text-xs text-muted-foreground">
                {CLASS_LABEL[enterprise.ownerClass]} · {enterprise.sector} · зарплата {enterprise.wageLevel}
              </p>
              <p className="mt-1 text-xs text-zinc-300">
                Статус: {enterpriseStatusLabel(enterprise)} · выпуск: {summarizeRecord(enterprise.producedResources)}
              </p>
            </div>
          ))}
        </div>
      </DetailsCard>

      <DetailsCard title="Рабочие" count={state.workers.length}>
        <div className="grid gap-2 sm:grid-cols-2">
          {state.workers.map((worker) => (
            <div key={worker.id} className={cn("rounded-md border p-2 text-xs", classTone(worker.classType))}>
              <p className="font-semibold">{workerLabel(worker)}</p>
              <p className="text-muted-foreground">{worker.location}{worker.enterpriseId ? ` · ${worker.enterpriseId}` : ""}</p>
            </div>
          ))}
        </div>
      </DetailsCard>

      <DetailsCard title="Карты и рынки" count={visibleBusinessDeals.length + enterpriseMarket(state).length}>
        <div className="space-y-2">
          {visibleBusinessDeals.map((deal) => (
            <div key={deal.id} className="rounded-md border border-amber-400/35 bg-amber-400/10 p-3">
              <p className="text-sm font-semibold text-amber-50">{deal.title}</p>
              <p className="text-xs text-zinc-300">
                {deal.requirements.map((req) => `${resourceLabel(req.resourceId)} ${req.amount}`).join(", ")}{" -> "}{deal.payout}
              </p>
            </div>
          ))}
          {enterpriseMarket(state).map((enterprise) => (
            <div key={`market-${enterprise.id}`} className="rounded-md border border-emerald-500/35 bg-emerald-500/10 p-3">
              <p className="text-sm font-semibold text-emerald-50">{enterprise.name ?? enterprise.id}</p>
              <p className="text-xs text-zinc-300">{CLASS_LABEL[enterprise.ownerClass]} · цена {enterprise.cost ?? 0}</p>
            </div>
          ))}
        </div>
      </DetailsCard>
    </div>
  );
}

function Metric({ label, value, compact = false }: { label: string; value: string; compact?: boolean }) {
  return (
    <div className={cn("rounded-md border border-zinc-700/70 bg-black/20 px-3 py-2", compact && "px-2 py-1.5")}>
      <p className={cn("truncate uppercase text-muted-foreground", compact ? "text-[9px]" : "text-[10px]")}>{label}</p>
      <p className={cn("truncate font-semibold text-zinc-50", compact ? "text-sm" : "text-base")}>{value}</p>
    </div>
  );
}

function DetailsCard({ title, count, children }: { title: string; count?: number; children: React.ReactNode }) {
  return (
    <details className="rounded-md border border-zinc-700/70 bg-card/92 p-3">
      <summary className="flex cursor-pointer list-none items-center justify-between gap-3">
        <span className="text-sm font-semibold uppercase tracking-[0.16em]">{title}</span>
        <span className="inline-flex items-center gap-2 text-xs text-muted-foreground">
          {count !== undefined ? count : ""}
          <ChevronDown className="h-4 w-4" />
        </span>
      </summary>
      <div className="mt-3">{children}</div>
    </details>
  );
}

function ActionsScreen({
  state,
  legalMoves,
  composerMetadata,
  isPreviewing,
  isSubmitting,
  lastPreviewResult,
  lastCommandResult,
  onPreview,
  onSubmit,
  operationError,
}: {
  state: GameState;
  legalMoves: LegalMove[];
  composerMetadata: ComposerMetadata;
  isPreviewing: boolean;
  isSubmitting: boolean;
  lastPreviewResult?: PreviewActionResponse;
  lastCommandResult?: CommandResponse;
  onPreview: (actionType: ActionType, parameters: Record<string, unknown>) => void;
  onSubmit: (actionType: ActionType, parameters: Record<string, unknown>) => void;
  operationError?: unknown;
}) {
  const [selectedId, setSelectedId] = useState("");
  const [selectedGroup, setSelectedGroup] = useState<ActionGroupId>("workers");
  const actionChoices = useMemo(() => {
    const byAction = new Map<ActionType, LegalMove>();
    legalMoves.forEach((move) => {
      if (!byAction.has(move.actionType)) {
        byAction.set(move.actionType, move);
      }
    });
    return [...byAction.values()];
  }, [legalMoves]);
  const moveCountByAction = useMemo(() => {
    const counts = new Map<ActionType, number>();
    legalMoves.forEach((move) => counts.set(move.actionType, (counts.get(move.actionType) ?? 0) + 1));
    return counts;
  }, [legalMoves]);
  const groupedActionChoices = useMemo(() => (
    ACTION_GROUPS.map((group) => ({
      ...group,
      moves: actionChoices.filter((move) => actionGroupId(move.actionType) === group.id),
    }))
  ), [actionChoices]);
  const firstAvailableGroup = groupedActionChoices.find((group) => group.moves.length > 0)?.id ?? "other";
  const activeGroup = groupedActionChoices.some((group) => group.id === selectedGroup && group.moves.length > 0)
    ? selectedGroup
    : firstAvailableGroup;
  const visibleActionChoices = groupedActionChoices.find((group) => group.id === activeGroup)?.moves ?? actionChoices;
  const selectedLegalMove = actionChoices.find((move) => move.id === selectedId);
  const selectedActionType = selectedLegalMove?.actionType;
  const [parameters, setParameters] = useState<Record<string, unknown>>({});

  useEffect(() => {
    if (activeGroup !== selectedGroup) {
      setSelectedGroup(activeGroup);
    }
  }, [activeGroup, selectedGroup]);

  useEffect(() => {
    const pool = visibleActionChoices.length > 0 ? visibleActionChoices : actionChoices;
    if (pool.length > 0 && !pool.some((move) => move.id === selectedId)) {
      setSelectedId(pool[0].id);
    }
  }, [actionChoices, selectedId, visibleActionChoices]);

  useEffect(() => {
    if (!selectedActionType) {
      return;
    }
    const seeded = seedParameters(selectedActionType, state, composerMetadata, selectedLegalMove);
    setParameters(seeded);
  }, [selectedActionType, selectedLegalMove?.id, selectedId]);

  const patchParameters = (patch: Record<string, unknown>) => {
    setParameters((prev) => ({ ...prev, ...patch }));
  };

  const selectedConsume = toString(parameters.consumeSelectedActionType, selectedActionType ?? "") as ActionType;
  const effectiveActionType = selectedActionType && CONSUME_ACTIONS.includes(selectedActionType) && CONSUME_ACTIONS.includes(selectedConsume)
    ? selectedConsume
    : selectedActionType;

  const preview = () => {
    if (effectiveActionType) {
      onPreview(effectiveActionType, parameters);
    }
  };

  const submit = () => {
    if (effectiveActionType) {
      onSubmit(effectiveActionType, parameters);
    }
  };

  return (
    <div className="space-y-3">
      <Card>
        <SectionTitle title="Ход" subtitle="Показаны только действия, разрешённые автономным движком в текущем состоянии." />
      </Card>

      <div className="grid grid-cols-3 gap-2">
        {groupedActionChoices.filter((group) => group.moves.length > 0).map((group) => (
          <button
            key={group.id}
            type="button"
            onClick={() => setSelectedGroup(group.id)}
            className={cn(
              "min-h-14 rounded-md border px-2 py-2 text-left transition",
              activeGroup === group.id ? "border-primary bg-primary/18 text-zinc-50" : "border-zinc-700/70 bg-black/20 text-zinc-300",
            )}
          >
            <p className="truncate text-xs font-semibold">{group.label}</p>
            <p className="text-[11px] text-muted-foreground">{group.moves.length}</p>
          </button>
        ))}
      </div>

      <div className="grid grid-cols-2 gap-2">
        {visibleActionChoices.map((move) => (
          <ActionChip
            key={move.id}
            active={selectedId === move.id}
            title={ACTION_LABEL[move.actionType]}
            subtitle={compactActionSummary(move.summary)}
            count={moveCountByAction.get(move.actionType) ?? 1}
            onClick={() => setSelectedId(move.id)}
          />
        ))}
      </div>

      {actionChoices.length === 0 && (
        <Card className="border-amber-400/45 bg-amber-400/10">
          <p className="text-sm text-amber-100">Сейчас движок не вернул legal moves.</p>
        </Card>
      )}

      {selectedActionType && (
        <Card>
          <SectionTitle title={ACTION_LABEL[selectedActionType]} subtitle={selectedLegalMove ? compactActionSummary(selectedLegalMove.summary) : undefined} />
          <div className="space-y-3">
            <ActionComposerFields
              actionType={selectedActionType}
              parameters={parameters}
              state={state}
              composerMetadata={composerMetadata}
              onPatch={patchParameters}
            />

            <div className="grid grid-cols-2 gap-2">
              <Button variant="secondary" onClick={preview} disabled={isPreviewing || isSubmitting}>
                {isPreviewing ? <SpinnerLabel label="Preview" /> : <><Eye className="h-4 w-4" /> Preview</>}
              </Button>
              <Button onClick={submit} disabled={isSubmitting || isPreviewing}>
                {isSubmitting ? <SpinnerLabel label="Submit" /> : <><Check className="h-4 w-4" /> Submit</>}
              </Button>
            </div>
          </div>
        </Card>
      )}

      <CommandFeedback preview={lastPreviewResult} command={lastCommandResult} />
      <OperationErrorPanel error={operationError} />
    </div>
  );
}

function ActionChip({ active, title, subtitle, count, onClick }: { active: boolean; title: string; subtitle: string; count?: number; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "min-h-24 rounded-md border p-3 text-left transition",
        active ? "border-primary bg-primary/18 text-zinc-50" : "border-zinc-700/70 bg-card/90 text-zinc-200",
      )}
    >
      <div className="flex items-start justify-between gap-2">
        <p className="line-clamp-2 text-sm font-semibold leading-5">{title}</p>
        {count && count > 1 && <Badge>{count}</Badge>}
      </div>
      <p className="mt-1 line-clamp-2 text-xs leading-4 text-muted-foreground">{subtitle}</p>
    </button>
  );
}

function ActionComposerFields({
  actionType,
  parameters,
  state,
  composerMetadata,
  onPatch,
}: {
  actionType: ActionType;
  parameters: Record<string, unknown>;
  state: GameState;
  composerMetadata: ComposerMetadata;
  onPatch: (patch: Record<string, unknown>) => void;
}) {
  const actorId = toString(parameters.actorPlayerId, actorPlayerId(state, composerMetadata));
  const actor = state.players.find((player) => player.playerId === actorId) ?? currentPlayer(state);

  return (
    <div className="space-y-3">
      <div className="space-y-1.5">
        <FieldLabel>Actor</FieldLabel>
        <SelectInput value={actorId} onChange={(event) => onPatch({ actorPlayerId: event.target.value })}>
          {state.players.map((player) => (
            <option key={player.playerId} value={player.playerId}>
              {CLASS_LABEL[player.classType]} ({player.playerId})
            </option>
          ))}
        </SelectInput>
      </div>

      {(actionType === "PROPOSE_BILL" || actionType === "CALL_EXTRAORDINARY_VOTE") && (
        <div className="grid gap-3 sm:grid-cols-2">
          <div className="space-y-1.5">
            <FieldLabel>Policy</FieldLabel>
            <SelectInput value={toString(parameters.policyId, state.policies[0]?.id ?? "")} onChange={(event) => onPatch({ policyId: event.target.value })}>
              {state.policies.map((policy) => (
                <option key={policy.id} value={policy.id}>
                  {POLICY_LABEL[policy.id]} ({policy.currentCourse})
                </option>
              ))}
            </SelectInput>
          </div>
          {actionType === "PROPOSE_BILL" && (
            <div className="space-y-1.5">
              <FieldLabel>Target</FieldLabel>
              <Segmented value={toString(parameters.targetCourse, "B")} values={["A", "B", "C"]} onChange={(value) => onPatch({ targetCourse: value })} />
            </div>
          )}
        </div>
      )}

      {actionType === "DECLARE_VOTE_STANCE" && (
        <div className="grid gap-3">
          <div className="space-y-1.5">
            <FieldLabel>Stance</FieldLabel>
            <Segmented value={toString(parameters.stance, "FOR")} values={["FOR", "AGAINST"]} onChange={(value) => onPatch({ stance: value })} />
          </div>
          <div className="space-y-1.5">
            <FieldLabel>Policy</FieldLabel>
            <SelectInput value={toString(parameters.policyId, state.currentVoteState?.activeProposalPolicyId ?? "")} onChange={(event) => onPatch({ policyId: event.target.value })}>
              {state.policies.map((policy) => (
                <option key={policy.id} value={policy.id}>
                  {POLICY_LABEL[policy.id]}
                </option>
              ))}
            </SelectInput>
          </div>
        </div>
      )}

      {actionType === "COMMIT_VOTE_INFLUENCE" && (
        <NumberField
          label={`Influence available: ${Math.max(0, actor?.influence ?? 0)}`}
          value={Math.max(0, toInt(parameters.influenceAmount, 0))}
          min={0}
          max={Math.max(0, actor?.influence ?? 0)}
          onChange={(value) => onPatch({ influenceAmount: value })}
        />
      )}

      {actionType === "DRAW_VOTING_CUBES" && (
        <NumberField
          label={`Bag: ${state.votingBag.worker}/${state.votingBag.middleClass}/${state.votingBag.capitalist}`}
          value={Math.max(1, toInt(parameters.count, 1))}
          min={1}
          max={Math.max(1, (state.votingBag.worker ?? 0) + (state.votingBag.middleClass ?? 0) + (state.votingBag.capitalist ?? 0))}
          onChange={(value) => onPatch({ count: value })}
        />
      )}

      {actionType === "ASSIGN_WORKERS" && (
        <CompactAssignmentEditor state={state} parameters={parameters} onPatch={onPatch} />
      )}

      {actionType === "BUY_GOODS_AND_SERVICES" && (
        <PurchaseEditor state={state} actor={actor} parameters={parameters} onPatch={onPatch} />
      )}

      {CONSUME_ACTIONS.includes(actionType) && (
        <ConsumeEditor state={state} actor={actor} parameters={parameters} onPatch={onPatch} />
      )}

      {actionType === "PLACE_STRIKES" && (
        <CheckboxList
          label="Enterprises"
          values={Array.isArray(parameters.enterpriseIds) ? parameters.enterpriseIds.map(String) : []}
          options={state.enterprises.map((enterprise) => ({ value: enterprise.id, label: enterprise.name ?? enterprise.id }))}
          onChange={(values) => onPatch({ enterpriseIds: values })}
        />
      )}

      {actionType === "PLACE_DEMONSTRATION" && (
        <PenaltyAllocationEditor state={state} parameters={parameters} onPatch={onPatch} />
      )}

      {actionType === "BUILD_ENTERPRISE" && (
        <BuildEnterpriseEditor state={state} parameters={parameters} onPatch={onPatch} />
      )}

      {actionType === "HIRE_WORKER" && (
        <NumberField label="Count" value={Math.max(0, toInt(parameters.count, 1))} min={0} max={5} onChange={(value) => onPatch({ count: value })} />
      )}

      {(actionType === "PRODUCE_GOODS" || actionType === "SELL_GOODS") && (
        <NumberField label="Amount" value={Math.max(0, toInt(parameters.amount, 1))} min={0} max={20} onChange={(value) => onPatch({ amount: value })} />
      )}

      {actionType === "ADJUST_POLICY" && (
        <div className="grid gap-3 sm:grid-cols-2">
          <div className="space-y-1.5">
            <FieldLabel>Track</FieldLabel>
            <SelectInput value={toString(parameters.track, "TAXATION")} onChange={(event) => onPatch({ track: event.target.value })}>
              <option value="TAXATION">TAXATION</option>
            </SelectInput>
          </div>
          <NumberField label="Delta" value={toInt(parameters.delta, 1)} min={-3} max={3} onChange={(value) => onPatch({ delta: value })} />
        </div>
      )}
    </div>
  );
}

function Segmented({ value, values, onChange }: { value: string; values: string[]; onChange: (value: string) => void }) {
  return (
    <div className="grid gap-1" style={{ gridTemplateColumns: `repeat(${values.length}, minmax(0, 1fr))` }}>
      {values.map((item) => (
        <button
          key={item}
          type="button"
          onClick={() => onChange(item)}
          className={cn(
            "min-h-11 rounded-md border px-2 text-sm font-semibold",
            value === item ? "border-primary bg-primary text-primary-foreground" : "border-zinc-700 bg-black/25 text-zinc-100",
          )}
        >
          {item}
        </button>
      ))}
    </div>
  );
}

function NumberField({
  label,
  value,
  min,
  max,
  onChange,
}: {
  label: string;
  value: number;
  min: number;
  max: number;
  onChange: (value: number) => void;
}) {
  const clamped = Math.min(max, Math.max(min, value));
  return (
    <div className="space-y-1.5">
      <FieldLabel>{label}</FieldLabel>
      <div className="grid grid-cols-[2.75rem,1fr,2.75rem] gap-2">
        <Button variant="outline" onClick={() => onChange(Math.max(min, clamped - 1))}>-</Button>
        <TextInput type="number" min={min} max={max} value={clamped} onChange={(event) => onChange(Math.min(max, Math.max(min, toInt(event.target.value, clamped))))} className="text-center" />
        <Button variant="outline" onClick={() => onChange(Math.min(max, clamped + 1))}>+</Button>
      </div>
    </div>
  );
}

function AssignmentEditor({
  state,
  parameters,
  onPatch,
}: {
  state: GameState;
  parameters: Record<string, unknown>;
  onPatch: (patch: Record<string, unknown>) => void;
}) {
  const assignments = Array.isArray(parameters.assignments) ? parameters.assignments.map(toRecord) : [];
  const actorId = toString(parameters.actorPlayerId, currentPlayer(state)?.playerId ?? "");
  const actor = state.players.find((player) => player.playerId === actorId) ?? currentPlayer(state);
  const classWorkers = state.workers.filter((worker) => worker.classType === actor?.classType);
  const selectedWorkerIds = new Set(assignments.map((assignment) => toString(assignment.workerId)).filter(Boolean));
  const selectedWorkers = classWorkers.filter((worker) => selectedWorkerIds.has(worker.id));

  const setAssignments = (nextAssignments: Array<Record<string, unknown>>) => {
    onPatch({ assignments: nextAssignments });
  };

  const toggleWorker = (workerId: string) => {
    if (selectedWorkerIds.has(workerId)) {
      setAssignments(assignments.filter((assignment) => toString(assignment.workerId) !== workerId));
      return;
    }
    if (assignments.length >= 3) {
      return;
    }
    setAssignments([
      ...assignments,
      {
        workerId,
        targetType: "UNEMPLOYED",
        targetId: "unemployed",
      },
    ]);
  };

  const workerFitsSlot = (worker: GameState["workers"][number], slot: GameState["enterprises"][number]["slots"][number]) => {
    if (slot.requiredQualification === "UNSKILLED") {
      return true;
    }
    if (worker.qualificationType !== "SKILLED") {
      return false;
    }
    return !slot.requiredSector || worker.sector === slot.requiredSector;
  };

  const assignSelectedToUnemployed = () => {
    setAssignments(assignments.map((assignment) => ({
      ...assignment,
      targetType: "UNEMPLOYED",
      targetId: "unemployed",
    })));
  };

  const assignSelectedToUnion = () => {
    setAssignments(assignments.map((assignment) => ({
      ...assignment,
      targetType: "UNION",
      targetId: "union",
    })));
  };

  const assignSelectedToEnterprise = (enterpriseId: string) => {
    if (selectedWorkers.length === 0) {
      return;
    }
    const enterprise = state.enterprises.find((item) => item.id === enterpriseId);
    if (!enterprise) {
      return;
    }
    const usedSlots = new Set<string>(
      assignments
        .map((assignment) => toString(assignment.targetId))
        .filter((targetId) => targetId.startsWith(`${enterpriseId}:`))
        .map((targetId) => targetId.split(":")[1] ?? ""),
    );
    const next = assignments.map((assignment) => {
      const worker = selectedWorkers.find((item) => item.id === toString(assignment.workerId));
      if (!worker) {
        return assignment;
      }
      const slot = enterprise.slots.find((candidate) => {
        if (usedSlots.has(candidate.id)) {
          return false;
        }
        if (candidate.occupiedWorkerId && candidate.occupiedWorkerId !== worker.id) {
          return false;
        }
        return workerFitsSlot(worker, candidate);
      });
      if (!slot) {
        return assignment;
      }
      usedSlots.add(slot.id);
      return {
        ...assignment,
        targetType: "ENTERPRISE_SLOT",
        targetId: `${enterprise.id}:${slot.id}`,
      };
    });
    setAssignments(next);
  };

  const assignFirstSelectedToSlot = (enterpriseId: string, slotId: string) => {
    const worker = selectedWorkers.find((candidate) => {
      const enterprise = state.enterprises.find((item) => item.id === enterpriseId);
      const slot = enterprise?.slots.find((item) => item.id === slotId);
      return slot ? workerFitsSlot(candidate, slot) : false;
    });
    if (!worker) {
      return;
    }
    setAssignments(assignments.map((assignment) =>
      toString(assignment.workerId) === worker.id
        ? { ...assignment, targetType: "ENTERPRISE_SLOT", targetId: `${enterpriseId}:${slotId}` }
        : assignment,
    ));
  };

  const assignmentForWorker = (workerId: string) => assignments.find((assignment) => toString(assignment.workerId) === workerId);
  const targetLabel = (assignment: Record<string, unknown> | undefined) => {
    if (!assignment) return "не выбран";
    const targetType = toString(assignment.targetType);
    const targetId = toString(assignment.targetId);
    if (targetType === "UNEMPLOYED") return "безработные";
    if (targetType === "UNION") return "профсоюз";
    const [enterpriseId, slotId] = targetId.split(":");
    const enterprise = state.enterprises.find((item) => item.id === enterpriseId);
    return `${enterprise?.name ?? enterpriseId} / ${slotId}`;
  };

  return (
    <div className="space-y-4">
      <div className="rounded-md border border-emerald-500/35 bg-emerald-500/10 p-3">
        <p className="text-sm font-semibold text-emerald-50">1. Выберите до 3 работников</p>
        <p className="mt-1 text-xs text-zinc-300">Тапните по карточкам работников текущего класса, затем выберите цель ниже.</p>
      </div>

      <div className="grid grid-cols-2 gap-2">
        {classWorkers.map((worker) => {
          const selected = selectedWorkerIds.has(worker.id);
          const assignment = assignmentForWorker(worker.id);
          return (
            <button
              key={worker.id}
              type="button"
              onClick={() => toggleWorker(worker.id)}
              disabled={!selected && assignments.length >= 3}
              className={cn(
                "min-h-24 rounded-md border p-3 text-left transition disabled:opacity-40",
                selected ? "border-primary bg-primary/15 text-zinc-50" : "border-zinc-700/70 bg-black/20 text-zinc-200",
              )}
            >
              <div className="flex items-start justify-between gap-2">
                <p className="text-sm font-semibold">{workerLabel(worker)}</p>
                {selected && <Check className="h-4 w-4 text-primary" />}
              </div>
              <p className="mt-1 text-xs text-muted-foreground">{worker.location}</p>
              <p className="mt-2 line-clamp-2 text-xs text-zinc-300">Цель: {targetLabel(assignment)}</p>
            </button>
          );
        })}
      </div>

      <div className="rounded-md border border-amber-400/35 bg-amber-400/10 p-3">
        <p className="text-sm font-semibold text-amber-50">2. Выберите цель</p>
        <p className="mt-1 text-xs text-zinc-300">Тап по предприятию автоматически разложит выбранных работников по подходящим свободным слотам.</p>
      </div>

      <div className="grid grid-cols-2 gap-2">
        <Button variant="outline" onClick={assignSelectedToUnemployed} disabled={selectedWorkers.length === 0}>Безработные</Button>
        <Button variant="outline" onClick={assignSelectedToUnion} disabled={selectedWorkers.length === 0}>Профсоюз</Button>
      </div>

      <div className="space-y-2">
        {state.enterprises.map((enterprise) => {
          const occupied = enterprise.slots.filter((slot) => slot.occupiedWorkerId).length;
          return (
            <div key={enterprise.id} className="rounded-md border border-zinc-700/70 bg-black/20 p-3">
              <button
                type="button"
                onClick={() => assignSelectedToEnterprise(enterprise.id)}
                disabled={selectedWorkers.length === 0}
                className="flex min-h-14 w-full items-center justify-between gap-3 text-left disabled:opacity-50"
              >
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold">{enterprise.name ?? enterprise.id}</p>
                  <p className="text-xs text-muted-foreground">{CLASS_LABEL[enterprise.ownerClass]} · {enterpriseStatusLabel(enterprise)}</p>
                </div>
                <Badge>{enterprise.wageLevel}</Badge>
              </button>
              <div className="mt-3 grid grid-cols-2 gap-2">
                {enterprise.slots.map((slot) => {
                  const selectedTarget = assignments.some((assignment) => toString(assignment.targetId) === `${enterprise.id}:${slot.id}`);
                  const occupiedBy = slot.occupiedWorkerId;
                  return (
                    <button
                      key={`${enterprise.id}-${slot.id}`}
                      type="button"
                      onClick={() => assignFirstSelectedToSlot(enterprise.id, slot.id)}
                      disabled={selectedWorkers.length === 0 || Boolean(occupiedBy)}
                      className={cn(
                        "min-h-16 rounded-md border px-2 py-2 text-left text-xs transition disabled:opacity-45",
                        selectedTarget ? "border-primary bg-primary/15 text-zinc-50" : "border-zinc-700/70 bg-zinc-950/70 text-zinc-200",
                      )}
                    >
                      <p className="font-semibold">{slot.id}</p>
                      <p className="text-muted-foreground">{slot.requiredQualification}{slot.requiredSector ? ` · ${slot.requiredSector}` : ""}</p>
                      <p className="mt-1">{occupiedBy ? `занят: ${occupiedBy}` : selectedTarget ? "выбрано" : "свободен"}</p>
                    </button>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>

      <div className="rounded-md border border-zinc-700/70 bg-black/25 p-3">
        <p className="text-xs font-semibold uppercase tracking-wide text-zinc-300">Будет отправлено</p>
        {assignments.length === 0 && <p className="mt-1 text-xs text-muted-foreground">Выберите работников и цель.</p>}
        {assignments.map((assignment) => (
          <p key={toString(assignment.workerId)} className="mt-1 text-xs text-zinc-200">
            {toString(assignment.workerId)}{" -> "}{targetLabel(assignment)}
          </p>
        ))}
      </div>
    </div>
  );
}

function CompactAssignmentEditor({
  state,
  parameters,
  onPatch,
}: {
  state: GameState;
  parameters: Record<string, unknown>;
  onPatch: (patch: Record<string, unknown>) => void;
}) {
  const assignments = Array.isArray(parameters.assignments) ? parameters.assignments.map(toRecord) : [];
  const actorId = toString(parameters.actorPlayerId, currentPlayer(state)?.playerId ?? "");
  const actor = state.players.find((player) => player.playerId === actorId) ?? currentPlayer(state);
  const classWorkers = state.workers.filter((worker) => worker.classType === actor?.classType);
  const selectedWorkerIds = new Set(assignments.map((assignment) => toString(assignment.workerId)).filter(Boolean));
  const selectedWorkers = classWorkers.filter((worker) => selectedWorkerIds.has(worker.id));
  const [selectedEnterpriseId, setSelectedEnterpriseId] = useState("");

  const setAssignments = (nextAssignments: Array<Record<string, unknown>>) => {
    onPatch({ assignments: nextAssignments });
  };

  const workerFitsSlot = (worker: GameState["workers"][number], slot: GameState["enterprises"][number]["slots"][number]) => {
    if (slot.requiredQualification === "UNSKILLED") {
      return true;
    }
    if (worker.qualificationType !== "SKILLED") {
      return false;
    }
    return !slot.requiredSector || worker.sector === slot.requiredSector;
  };

  const toggleWorker = (workerId: string) => {
    if (selectedWorkerIds.has(workerId)) {
      setAssignments(assignments.filter((assignment) => toString(assignment.workerId) !== workerId));
      return;
    }
    if (assignments.length >= 3) {
      return;
    }
    setAssignments([
      ...assignments,
      { workerId, targetType: "UNEMPLOYED", targetId: "unemployed" },
    ]);
  };

  const assignSelectedToUnemployed = () => {
    setAssignments(assignments.map((assignment) => ({
      ...assignment,
      targetType: "UNEMPLOYED",
      targetId: "unemployed",
    })));
  };

  const assignSelectedToUnion = () => {
    setAssignments(assignments.map((assignment) => ({
      ...assignment,
      targetType: "UNION",
      targetId: "union",
    })));
  };

  const assignSelectedToEnterprise = (enterpriseId: string) => {
    const enterprise = state.enterprises.find((item) => item.id === enterpriseId);
    if (!enterprise || selectedWorkers.length === 0) {
      return;
    }
    const usedSlots = new Set<string>(
      assignments
        .map((assignment) => toString(assignment.targetId))
        .filter((targetId) => targetId.startsWith(`${enterpriseId}:`))
        .map((targetId) => targetId.split(":")[1] ?? ""),
    );
    const next = assignments.map((assignment) => {
      const worker = selectedWorkers.find((item) => item.id === toString(assignment.workerId));
      if (!worker) {
        return assignment;
      }
      const slot = enterprise.slots.find((candidate) => {
        if (usedSlots.has(candidate.id)) {
          return false;
        }
        if (candidate.occupiedWorkerId && candidate.occupiedWorkerId !== worker.id) {
          return false;
        }
        return workerFitsSlot(worker, candidate);
      });
      if (!slot) {
        return assignment;
      }
      usedSlots.add(slot.id);
      return {
        ...assignment,
        targetType: "ENTERPRISE_SLOT",
        targetId: `${enterprise.id}:${slot.id}`,
      };
    });
    setAssignments(next);
  };

  const assignFirstSelectedToSlot = (enterpriseId: string, slotId: string) => {
    const enterprise = state.enterprises.find((item) => item.id === enterpriseId);
    const slot = enterprise?.slots.find((item) => item.id === slotId);
    if (!slot) {
      return;
    }
    const worker = selectedWorkers.find((candidate) => workerFitsSlot(candidate, slot));
    if (!worker) {
      return;
    }
    setAssignments(assignments.map((assignment) =>
      toString(assignment.workerId) === worker.id
        ? { ...assignment, targetType: "ENTERPRISE_SLOT", targetId: `${enterpriseId}:${slotId}` }
        : assignment,
    ));
  };

  const targetLabel = (assignment: Record<string, unknown> | undefined) => {
    if (!assignment) return "не выбрано";
    const targetType = toString(assignment.targetType);
    const targetId = toString(assignment.targetId);
    if (targetType === "UNEMPLOYED") return "безработные";
    if (targetType === "UNION") return "профсоюз";
    const [enterpriseId, slotId] = targetId.split(":");
    const enterprise = state.enterprises.find((item) => item.id === enterpriseId);
    return `${enterprise?.name ?? enterpriseId} / ${slotId}`;
  };

  const enterpriseRows = state.enterprises
    .map((enterprise) => {
      const occupied = enterprise.slots.filter((slot) => slot.occupiedWorkerId).length;
      const openSlots = enterprise.slots.filter((slot) => !slot.occupiedWorkerId);
      const compatibleSlots = openSlots.filter((slot) => selectedWorkers.length === 0 || selectedWorkers.some((worker) => workerFitsSlot(worker, slot)));
      const selectedTargets = assignments.filter((assignment) => toString(assignment.targetId).startsWith(`${enterprise.id}:`)).length;
      return { enterprise, occupied, openSlots: openSlots.length, compatibleSlots: compatibleSlots.length, selectedTargets };
    })
    .sort((left, right) => {
      const leftUseful = left.compatibleSlots > 0 ? 1 : 0;
      const rightUseful = right.compatibleSlots > 0 ? 1 : 0;
      return rightUseful - leftUseful || right.compatibleSlots - left.compatibleSlots || right.openSlots - left.openSlots;
    });
  const visibleEnterpriseRows = enterpriseRows.filter((row) => selectedWorkers.length === 0 || row.compatibleSlots > 0);
  const effectiveEnterpriseRow =
    visibleEnterpriseRows.find((row) => row.enterprise.id === selectedEnterpriseId)
    ?? visibleEnterpriseRows[0]
    ?? enterpriseRows[0];
  const selectedEnterprise = effectiveEnterpriseRow?.enterprise;

  return (
    <div className="space-y-3">
      <div className="grid grid-cols-3 gap-2">
        <Metric label="Выбрано" value={`${assignments.length}/3`} compact />
        <Metric label="Рабочие" value={String(classWorkers.length)} compact />
        <Metric label="Класс" value={actor ? CLASS_SHORT[actor.classType] : "-"} compact />
      </div>

      <div className="no-scrollbar -mx-3 flex gap-2 overflow-x-auto px-3 pb-1">
        {classWorkers.map((worker) => {
          const selected = selectedWorkerIds.has(worker.id);
          const assignment = assignments.find((item) => toString(item.workerId) === worker.id);
          return (
            <button
              key={worker.id}
              type="button"
              onClick={() => toggleWorker(worker.id)}
              disabled={!selected && assignments.length >= 3}
              className={cn(
                "min-h-24 w-40 shrink-0 rounded-md border p-3 text-left transition disabled:opacity-40",
                selected ? "border-primary bg-primary/15 text-zinc-50" : "border-zinc-700/70 bg-black/20 text-zinc-200",
              )}
            >
              <div className="flex items-start justify-between gap-2">
                <p className="line-clamp-2 text-sm font-semibold">{workerLabel(worker)}</p>
                {selected && <Check className="h-4 w-4 shrink-0 text-primary" />}
              </div>
              <p className="mt-1 text-xs text-muted-foreground">{worker.location}</p>
              <p className="mt-2 line-clamp-2 text-xs text-zinc-300">{targetLabel(assignment)}</p>
            </button>
          );
        })}
      </div>

      <div className="grid grid-cols-2 gap-2">
        <Button variant="outline" onClick={assignSelectedToUnemployed} disabled={selectedWorkers.length === 0}>Безработные</Button>
        <Button variant="outline" onClick={assignSelectedToUnion} disabled={selectedWorkers.length === 0}>Профсоюз</Button>
      </div>

      <div className="grid max-h-60 grid-cols-2 gap-2 overflow-y-auto pr-1">
        {visibleEnterpriseRows.map(({ enterprise, occupied, compatibleSlots, selectedTargets }) => {
          const active = enterprise.id === selectedEnterprise?.id;
          return (
            <button
              key={enterprise.id}
              type="button"
              onClick={() => {
                setSelectedEnterpriseId(enterprise.id);
                assignSelectedToEnterprise(enterprise.id);
              }}
              className={cn(
                "min-h-24 rounded-md border p-3 text-left transition",
                active ? "border-primary bg-primary/15 text-zinc-50" : "border-zinc-700/70 bg-black/20 text-zinc-200",
              )}
            >
              <div className="flex items-start justify-between gap-2">
                <p className="line-clamp-2 text-sm font-semibold">{enterprise.name ?? enterprise.id}</p>
                {selectedTargets > 0 && <Badge tone="good">{selectedTargets}</Badge>}
              </div>
              <p className="mt-1 text-xs text-muted-foreground">{CLASS_SHORT[enterprise.ownerClass]} · {enterprise.sector}</p>
              <p className="mt-2 text-xs text-zinc-300">{enterpriseStatusLabel(enterprise)} · мест: {compatibleSlots}</p>
            </button>
          );
        })}
      </div>

      {selectedEnterprise && (
        <div className="rounded-md border border-zinc-700/70 bg-black/25 p-3">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold">{selectedEnterprise.name ?? selectedEnterprise.id}</p>
              <p className="text-xs text-muted-foreground">{CLASS_LABEL[selectedEnterprise.ownerClass]} · зарплата {selectedEnterprise.wageLevel}</p>
            </div>
            <Button className="shrink-0" variant="secondary" onClick={() => assignSelectedToEnterprise(selectedEnterprise.id)} disabled={selectedWorkers.length === 0}>
              <Check className="h-4 w-4" />
              Назначить
            </Button>
          </div>
          <div className="mt-3 grid grid-cols-2 gap-2">
            {selectedEnterprise.slots.map((slot) => {
              const selectedTarget = assignments.some((assignment) => toString(assignment.targetId) === `${selectedEnterprise.id}:${slot.id}`);
              const occupiedBy = slot.occupiedWorkerId;
              const fitsAny = selectedWorkers.length === 0 || selectedWorkers.some((worker) => workerFitsSlot(worker, slot));
              return (
                <button
                  key={`${selectedEnterprise.id}-${slot.id}`}
                  type="button"
                  onClick={() => assignFirstSelectedToSlot(selectedEnterprise.id, slot.id)}
                  disabled={selectedWorkers.length === 0 || Boolean(occupiedBy) || !fitsAny}
                  className={cn(
                    "min-h-16 rounded-md border px-2 py-2 text-left text-xs transition disabled:opacity-40",
                    selectedTarget ? "border-primary bg-primary/15 text-zinc-50" : "border-zinc-700/70 bg-zinc-950/70 text-zinc-200",
                  )}
                >
                  <p className="font-semibold">{slot.id}</p>
                  <p className="text-muted-foreground">{slot.requiredQualification}{slot.requiredSector ? ` · ${slot.requiredSector}` : ""}</p>
                  <p className="mt-1">{occupiedBy ? `занят: ${occupiedBy}` : selectedTarget ? "выбрано" : "свободен"}</p>
                </button>
              );
            })}
          </div>
        </div>
      )}

      <div className="rounded-md border border-zinc-700/70 bg-black/25 p-3">
        <p className="text-xs font-semibold uppercase tracking-wide text-zinc-300">Назначения</p>
        {assignments.length === 0 && <p className="mt-1 text-xs text-muted-foreground">Нет выбранных работников.</p>}
        {assignments.map((assignment) => (
          <p key={toString(assignment.workerId)} className="mt-1 text-xs text-zinc-200">
            {toString(assignment.workerId)}{" -> "}{targetLabel(assignment)}
          </p>
        ))}
      </div>
    </div>
  );
}

function PurchaseEditor({
  state,
  actor,
  parameters,
  onPatch,
}: {
  state: GameState;
  actor?: GameState["players"][number];
  parameters: Record<string, unknown>;
  onPatch: (patch: Record<string, unknown>) => void;
}) {
  const resourceType = toString(parameters.resourceType, "FOOD");
  const options = supplierOptions(state, actor, resourceType);
  const quantityMap = toRecord(parameters.buyQuantityBySupplier);
  const priceMap = toRecord(parameters.buyPriceBySupplier);
  const patchQuantity = (key: string, quantity: number) => {
    const nextQuantity = { ...quantityMap, [key]: quantity };
    const nextParameters = {
      buyQuantityBySupplier: nextQuantity,
      purchases: purchaseRowsFromMaps({ ...parameters, buyQuantityBySupplier: nextQuantity }, options),
    };
    onPatch(nextParameters);
  };
  const patchPrice = (key: string, price: number) => {
    const nextPrice = { ...priceMap, [key]: price };
    onPatch({
      buyPriceBySupplier: nextPrice,
      purchases: purchaseRowsFromMaps({ ...parameters, buyPriceBySupplier: nextPrice }, options),
    });
  };
  const total = purchaseRowsFromMaps(parameters, options).reduce((sum, row) => sum + row.quantity * (row.unitPriceOverride ?? 0), 0);

  return (
    <div className="space-y-3">
      <div className="space-y-1.5">
        <FieldLabel>Resource</FieldLabel>
        <SelectInput
          value={resourceType}
          onChange={(event) => onPatch({ resourceType: event.target.value, purchases: [], buyQuantityBySupplier: {}, buyPriceBySupplier: {} })}
        >
          <option value="FOOD">FOOD</option>
          <option value="HEALTHCARE">HEALTHCARE</option>
          <option value="EDUCATION">EDUCATION</option>
          <option value="LUXURY">LUXURY</option>
        </SelectInput>
      </div>
      <Metric label="Total cost" value={String(total)} />
      {options.map((option) => {
        const maxAllowed = Math.min(option.available, Math.max(0, actor?.population ?? 0));
        const quantity = Math.min(maxAllowed, Math.max(0, toInt(quantityMap[option.key], 0)));
        const price = Math.max(0, toInt(priceMap[option.key], option.unitPrice));
        return (
          <div key={option.key} className="space-y-2 rounded-md border border-zinc-700/70 bg-black/20 p-3">
            <div className="flex items-start justify-between gap-2">
              <div>
                <p className="text-sm font-semibold">{option.label}</p>
                <p className="text-xs text-muted-foreground">Доступно {option.available} · цена {option.unitPrice}</p>
              </div>
              <Badge>{quantity}</Badge>
            </div>
            <NumberField label="Quantity" value={quantity} min={0} max={maxAllowed} onChange={(value) => patchQuantity(option.key, value)} />
            <NumberField label="Unit price" value={price} min={0} max={99} onChange={(value) => patchPrice(option.key, value)} />
          </div>
        );
      })}
      {options.length === 0 && <p className="text-xs text-rose-300">Нет доступных поставщиков.</p>}
    </div>
  );
}

function ConsumeEditor({
  state,
  actor,
  parameters,
  onPatch,
}: {
  state: GameState;
  actor?: GameState["players"][number];
  parameters: Record<string, unknown>;
  onPatch: (patch: Record<string, unknown>) => void;
}) {
  const selected = toString(parameters.consumeSelectedActionType, "CONSUME_LUXURY");
  const storageKey = selected === "CONSUME_LUXURY" ? "luxury" : selected === "CONSUME_EDUCATION" ? "education" : "healthcare";
  const required = Math.max(0, actor?.population ?? 0);
  const available = Math.max(0, toInt(actor?.goodsAndServicesArea?.[storageKey] ?? actor?.producedResourceStorage?.[storageKey], 0));
  const trainableWorkers = state.workers.filter((worker) => worker.qualificationType === "UNSKILLED" && (!actor || worker.classType === actor.classType));

  return (
    <div className="space-y-3">
      <div className="space-y-1.5">
        <FieldLabel>Consume</FieldLabel>
        <SelectInput value={selected} onChange={(event) => onPatch({ consumeSelectedActionType: event.target.value })}>
          <option value="CONSUME_LUXURY">LUXURY</option>
          <option value="CONSUME_EDUCATION">EDUCATION</option>
          <option value="CONSUME_HEALTHCARE">HEALTHCARE</option>
        </SelectInput>
      </div>
      <div className="grid grid-cols-2 gap-2">
        <Metric label="Required" value={String(required)} />
        <Metric label="Available" value={String(available)} />
      </div>
      {selected === "CONSUME_EDUCATION" && (
        <div className="grid gap-3">
          <div className="space-y-1.5">
            <FieldLabel>Worker</FieldLabel>
            <SelectInput value={toString(parameters.workerId, trainableWorkers[0]?.id ?? "")} onChange={(event) => onPatch({ workerId: event.target.value })}>
              {trainableWorkers.map((worker) => (
                <option key={worker.id} value={worker.id}>{workerLabel(worker)}</option>
              ))}
            </SelectInput>
          </div>
          <div className="space-y-1.5">
            <FieldLabel>Target color</FieldLabel>
            <SelectInput value={toString(parameters.targetColor, "WHITE")} onChange={(event) => onPatch({ targetColor: event.target.value })}>
              {["WHITE", "GREEN", "BLUE", "ORANGE", "PURPLE"].map((color) => (
                <option key={color} value={color}>{color}</option>
              ))}
            </SelectInput>
          </div>
        </div>
      )}
    </div>
  );
}

function PenaltyAllocationEditor({
  state,
  parameters,
  onPatch,
}: {
  state: GameState;
  parameters: Record<string, unknown>;
  onPatch: (patch: Record<string, unknown>) => void;
}) {
  const allocation = toRecord(parameters.penaltyAllocation);
  return (
    <div className="space-y-2">
      <FieldLabel>Penalty allocation</FieldLabel>
      {state.players.map((player) => (
        <NumberField
          key={player.playerId}
          label={CLASS_LABEL[player.classType]}
          value={Math.max(0, toInt(allocation[player.playerId], 0))}
          min={0}
          max={10}
          onChange={(value) => onPatch({ penaltyAllocation: { ...allocation, [player.playerId]: value } })}
        />
      ))}
    </div>
  );
}

function BuildEnterpriseEditor({
  state,
  parameters,
  onPatch,
}: {
  state: GameState;
  parameters: Record<string, unknown>;
  onPatch: (patch: Record<string, unknown>) => void;
}) {
  const market = enterpriseMarket(state);
  const selected = market.find((enterprise) => enterprise.id === parameters.enterpriseId) ?? market[0];
  return (
    <div className="space-y-3">
      <div className="space-y-1.5">
        <FieldLabel>Enterprise</FieldLabel>
        <SelectInput
          value={toString(parameters.enterpriseId, selected?.id ?? "")}
          onChange={(event) => {
            const enterprise = market.find((item) => item.id === event.target.value);
            onPatch({ enterpriseId: event.target.value, cost: enterprise?.cost ?? 20 });
          }}
        >
          {market.map((enterprise) => (
            <option key={enterprise.id} value={enterprise.id}>
              {enterprise.name ?? enterprise.id} ({CLASS_SHORT[enterprise.ownerClass]})
            </option>
          ))}
        </SelectInput>
      </div>
      <NumberField label="Cost" value={Math.max(0, toInt(parameters.cost, selected?.cost ?? 20))} min={0} max={200} onChange={(value) => onPatch({ cost: value })} />
      <NumberField label="Wage level" value={Math.max(1, toInt(parameters.wageLevel, 2))} min={1} max={3} onChange={(value) => onPatch({ wageLevel: value })} />
    </div>
  );
}

function CheckboxList({
  label,
  values,
  options,
  onChange,
}: {
  label: string;
  values: string[];
  options: Array<{ value: string; label: string }>;
  onChange: (values: string[]) => void;
}) {
  const selected = new Set(values);
  return (
    <div className="space-y-2">
      <FieldLabel>{label}</FieldLabel>
      <div className="space-y-2">
        {options.map((option) => (
          <button
            key={option.value}
            type="button"
            onClick={() => {
              const next = new Set(selected);
              if (next.has(option.value)) {
                next.delete(option.value);
              } else {
                next.add(option.value);
              }
              onChange([...next]);
            }}
            className={cn(
              "flex min-h-11 w-full items-center justify-between gap-3 rounded-md border px-3 text-left text-sm",
              selected.has(option.value) ? "border-primary bg-primary/15 text-zinc-50" : "border-zinc-700 bg-black/20 text-zinc-200",
            )}
          >
            <span>{option.label}</span>
            {selected.has(option.value) && <Check className="h-4 w-4" />}
          </button>
        ))}
      </div>
    </div>
  );
}

function CommandFeedback({ preview, command }: { preview?: PreviewActionResponse; command?: CommandResponse }) {
  if (!preview && !command) {
    return null;
  }
  return (
    <div className="space-y-3">
      {preview && (
        <Card className={preview.accepted ? "border-emerald-500/45 bg-emerald-500/10" : "border-rose-500/45 bg-rose-500/10"}>
          <SectionTitle title="Preview" />
          {preview.accepted ? (
            <div className="space-y-1 text-xs leading-5 text-zinc-200">
              <p>Money: {stringifyUnknown(preview.delta.moneyDeltaByPlayer)}</p>
              <p>Resources: {stringifyUnknown(preview.delta.resourceDeltaByPlayer)}</p>
              <p>Workers: {stringifyUnknown(preview.delta.workerMovement)}</p>
              {preview.delta.notes.length > 0 && <p>Notes: {preview.delta.notes.join(" | ")}</p>}
            </div>
          ) : (
            <ErrorList errors={preview.errors} />
          )}
        </Card>
      )}
      {command && (
        <Card className={command.accepted ? "border-emerald-500/45 bg-emerald-500/10" : "border-rose-500/45 bg-rose-500/10"}>
          <SectionTitle title="Command" />
          {command.accepted ? (
            <div className="space-y-1 text-xs leading-5 text-zinc-200">
              {command.events.length === 0 && <p>Accepted.</p>}
              {command.events.map((event, index) => (
                <p key={`${event.type}-${index}`}>{event.description}</p>
              ))}
            </div>
          ) : (
            <ErrorList errors={command.errors} />
          )}
        </Card>
      )}
    </div>
  );
}

function OperationErrorPanel({ error }: { error?: unknown }) {
  if (!error) {
    return null;
  }
  return (
    <Card className="border-rose-500/45 bg-rose-500/10">
      <SectionTitle title="Operation error" />
      <p className="break-words text-xs leading-5 text-rose-100">{error instanceof Error ? error.message : String(error)}</p>
    </Card>
  );
}

function ErrorList({ errors }: { errors: string[] }) {
  return (
    <ul className="space-y-1 text-xs leading-5 text-rose-100">
      {errors.map((error, index) => (
        <li key={`${error}-${index}`}>{error}</li>
      ))}
    </ul>
  );
}

function BotScreen({
  state,
  lastBotSummary,
  isBotTurnLoading,
  isBotUntilLoading,
  onPlayBotTurn,
  onPlayBotUntilHuman,
  operationError,
}: {
  state: GameState;
  lastBotSummary?: BotTurnSummary;
  isBotTurnLoading: boolean;
  isBotUntilLoading: boolean;
  onPlayBotTurn: () => void;
  onPlayBotUntilHuman: () => void;
  operationError?: unknown;
}) {
  const actor = currentPlayer(state);
  const canPlayBot = actor?.controlMode === "BOT";
  return (
    <div className="space-y-3">
      <Card>
        <SectionTitle title="Bot control" />
        <div className="grid gap-2">
          <Button disabled={!canPlayBot || isBotTurnLoading} onClick={onPlayBotTurn}>
            {isBotTurnLoading ? <SpinnerLabel label="Bot turn" /> : <><Play className="h-4 w-4" /> Bot turn</>}
          </Button>
          <Button variant="secondary" disabled={isBotUntilLoading} onClick={onPlayBotUntilHuman}>
            {isBotUntilLoading ? <SpinnerLabel label="Until human" /> : <><Bot className="h-4 w-4" /> Until human</>}
          </Button>
        </div>
      </Card>
      <Card>
        <SectionTitle title="Current actor" />
        <div className={cn("rounded-md border p-3", classTone(actor?.classType))}>
          <p className="text-sm font-semibold">{actor ? CLASS_LABEL[actor.classType] : "н/д"}</p>
          <p className="text-xs text-muted-foreground">{actor?.controlMode ?? "н/д"} · {actor?.botStrategyMode ?? "н/д"}</p>
        </div>
      </Card>
      <Card>
        <SectionTitle title="Last bot move" />
        {!lastBotSummary && <p className="text-sm text-muted-foreground">Нет хода бота.</p>}
        {lastBotSummary && (
          <div className="space-y-2 text-sm">
            <Metric label="Action" value={ACTION_LABEL[lastBotSummary.selectedAction]} />
            <Metric label="Planner" value={lastBotSummary.plannerId} />
            <p className="rounded-md border border-zinc-700/70 bg-black/20 p-3 text-xs leading-5 text-zinc-200">{lastBotSummary.rationale}</p>
            <p className="text-xs text-muted-foreground">Legal options: {lastBotSummary.legalOptionsConsidered}</p>
          </div>
        )}
      </Card>
      <OperationErrorPanel error={operationError} />
    </div>
  );
}

function LogScreen({ state }: { state: GameState }) {
  const events = [...state.eventLog].reverse();
  return (
    <div className="space-y-3">
      <Card>
        <SectionTitle title="Event log" />
        {events.length === 0 && <p className="text-sm text-muted-foreground">Событий нет.</p>}
        <div className="space-y-2">
          {events.map((event) => (
            <div key={event.id} className="rounded-md border border-zinc-700/70 bg-black/20 p-3">
              <div className="flex items-start justify-between gap-2">
                <p className="text-xs font-semibold uppercase tracking-wide text-amber-200">{event.type}</p>
                <span className="text-xs text-muted-foreground">#{event.id}</span>
              </div>
              <p className="mt-1 text-sm leading-5 text-zinc-100">{event.message}</p>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}

function SetupScreen({
  state,
  saveFileName,
  setSaveFileName,
  isSaving,
  isLoading,
  isResetting,
  isApplyingSetup,
  onApplySetup,
  onSave,
  onLoad,
  onReset,
  operationError,
}: {
  state: GameState;
  saveFileName: string;
  setSaveFileName: (value: string) => void;
  isSaving: boolean;
  isLoading: boolean;
  isResetting: boolean;
  isApplyingSetup: boolean;
  onApplySetup: (payload: SetupPayload) => void;
  onSave: () => void;
  onLoad: () => void;
  onReset: () => void;
  operationError?: unknown;
}) {
  const activeClasses = state.turnOrder.activeClasses ?? state.players.map((player) => player.classType);
  const [playerCount, setPlayerCount] = useState(String(Math.max(2, Math.min(4, activeClasses.length || 4))));
  const [controlModes, setControlModes] = useState<Record<string, PlayerControlMode>>({});

  useEffect(() => {
    setControlModes(Object.fromEntries(state.players.map((player) => [player.classType, player.controlMode])) as Record<string, PlayerControlMode>);
    setPlayerCount(String(Math.max(2, Math.min(4, activeClasses.length || 4))));
  }, [state.players, activeClasses.length]);

  const selectedCount = Math.max(2, Math.min(4, toInt(playerCount, 4)));
  const editableClasses = PLAYER_CLASS_ORDER_BY_COUNT[selectedCount];

  const applySetup = () => {
    const filteredControlModes = editableClasses.reduce<Record<string, string>>((acc, classType) => {
      acc[classType] = classType === "STATE" ? "HUMAN" : controlModes[classType] ?? "HUMAN";
      return acc;
    }, {});
    const botStrategyModes = editableClasses.reduce<Record<string, string>>((acc, classType) => {
      acc[classType] = filteredControlModes[classType] === "BOT" ? "CARD_DRIVEN_SIMPLE_AUTOMA" : "HEURISTIC_FALLBACK";
      return acc;
    }, {});
    onApplySetup({ playerCount: selectedCount, controlModes: filteredControlModes, botStrategyModes });
  };

  return (
    <div className="space-y-3">
      <Card>
        <SectionTitle title="Настройка партии" subtitle="Работает полностью на телефоне: состояние, правила, бот и сохранения находятся внутри APK." />
        <div className="space-y-3">
          <div className="space-y-1.5">
            <FieldLabel>Players</FieldLabel>
            <Segmented value={playerCount} values={["2", "3", "4"]} onChange={setPlayerCount} />
          </div>
          {editableClasses.map((classType) => (
            <div key={classType} className="grid grid-cols-[1fr,9rem] items-center gap-2 rounded-md border border-zinc-700/70 bg-black/20 p-3">
              <span className="text-sm font-semibold">{CLASS_LABEL[classType]}</span>
              <SelectInput
                value={classType === "STATE" ? "HUMAN" : controlModes[classType] ?? "HUMAN"}
                disabled={classType === "STATE"}
                onChange={(event) => setControlModes((prev) => ({ ...prev, [classType]: event.target.value as PlayerControlMode }))}
              >
                <option value="HUMAN">HUMAN</option>
                {classType !== "STATE" && <option value="BOT">BOT</option>}
              </SelectInput>
            </div>
          ))}
          <Button className="w-full" onClick={applySetup} disabled={isApplyingSetup}>
            {isApplyingSetup ? <SpinnerLabel label="Apply" /> : <><Check className="h-4 w-4" /> Apply setup</>}
          </Button>
        </div>
      </Card>

      <Card>
        <SectionTitle title="Save / load" />
        <div className="space-y-2">
          <TextInput value={saveFileName} onChange={(event) => setSaveFileName(event.target.value)} />
          <div className="grid grid-cols-2 gap-2">
            <Button variant="secondary" onClick={onSave} disabled={isSaving}>
              {isSaving ? <SpinnerLabel label="Save" /> : <><Save className="h-4 w-4" /> Save</>}
            </Button>
            <Button variant="outline" onClick={onLoad} disabled={isLoading}>
              {isLoading ? <SpinnerLabel label="Load" /> : <><Upload className="h-4 w-4" /> Load</>}
            </Button>
          </div>
          <Button className="w-full" variant="danger" onClick={onReset} disabled={isResetting}>
            {isResetting ? <SpinnerLabel label="Reset" /> : <><RotateCcw className="h-4 w-4" /> Reset</>}
          </Button>
        </div>
      </Card>
      <OperationErrorPanel error={operationError} />
    </div>
  );
}

export function App() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<TabKey>("state");
  const [saveFileName, setSaveFileName] = useState("mobile-save");
  const [lastPreviewResult, setLastPreviewResult] = useState<PreviewActionResponse | undefined>();
  const [lastCommandResult, setLastCommandResult] = useState<CommandResponse | undefined>();
  const [lastBotSummary, setLastBotSummary] = useState<BotTurnSummary | undefined>();
  const api = useMemo(() => createGameApi(), []);

  const gameQuery = useQuery({
    queryKey: ["mobile-game"],
    queryFn: api.getGame,
  });

  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ["mobile-game"] });
  };

  const playUntilHumanMutation = useMutation({
    mutationFn: api.playBotUntilHuman,
    onSuccess: async (result) => {
      setLastBotSummary(result.turnSummaries.at(-1));
      await refresh();
    },
  });

  const commandMutation = useMutation({
    mutationFn: ({ actionType, parameters }: { actionType: ActionType; parameters: Record<string, unknown> }) =>
      api.submitCommand({ actionType, parameters }),
    onSuccess: async (result, variables) => {
      setLastCommandResult(result);
      if (result.accepted) {
        setLastPreviewResult(undefined);
      }

      const nextState = result.gameState;
      const nextIndex = Number.isInteger(nextState.turnOrder?.currentPlayerIndex) ? Number(nextState.turnOrder.currentPlayerIndex) : 0;
      const nextActor = nextState.players[nextIndex];

      if (result.accepted && variables.actionType === "END_TURN" && nextActor?.controlMode === "BOT") {
        await playUntilHumanMutation.mutateAsync();
        return;
      }

      await refresh();
    },
  });

  const previewMutation = useMutation({
    mutationFn: ({ actionType, parameters }: { actionType: ActionType; parameters: Record<string, unknown> }) =>
      api.previewAction({ actionType, parameters }),
    onSuccess: (result) => setLastPreviewResult(result),
  });

  const playBotTurnMutation = useMutation({
    mutationFn: api.playBotTurn,
    onSuccess: async (result) => {
      setLastBotSummary(result.summary);
      await refresh();
    },
  });

  const setupMutation = useMutation({
    mutationFn: api.setupGame,
    onSuccess: async () => {
      setLastCommandResult(undefined);
      setLastPreviewResult(undefined);
      setLastBotSummary(undefined);
      await refresh();
    },
  });

  const saveMutation = useMutation({ mutationFn: () => api.saveGame(saveFileName) });
  const loadMutation = useMutation({
    mutationFn: () => api.loadGame(saveFileName),
    onSuccess: refresh,
  });
  const resetMutation = useMutation({
    mutationFn: api.resetGame,
    onSuccess: async () => {
      setLastCommandResult(undefined);
      setLastPreviewResult(undefined);
      setLastBotSummary(undefined);
      await refresh();
    },
  });

  const state = gameQuery.data?.gameState;
  const legalMoves = gameQuery.data?.legalMoves ?? [];
  const composerMetadata = gameQuery.data?.composerMetadata;

  return (
    <div className="min-h-screen text-foreground">
      <AppTopBar state={state} />
      <main className="mx-auto w-full max-w-3xl px-3 pb-24 pt-3">
        {gameQuery.isLoading && (
          <Card>
            <SpinnerLabel label="Loading game state" />
          </Card>
        )}

        {gameQuery.isError && (
          <Card className="border-rose-500/45 bg-rose-500/10">
            <SectionTitle title="Ошибка автономного движка" />
            <p className="break-words text-sm leading-5 text-rose-100">{String(gameQuery.error)}</p>
            <Button className="mt-3 w-full" variant="outline" onClick={() => void gameQuery.refetch()}>
              <RefreshCw className="h-4 w-4" />
              Retry
            </Button>
          </Card>
        )}

        {state && composerMetadata && (
          <>
            {activeTab === "state" && <StateScreen state={state} />}
            {activeTab === "actions" && (
              <ActionsScreen
                state={state}
                legalMoves={legalMoves}
                composerMetadata={composerMetadata}
                isPreviewing={previewMutation.isPending}
                isSubmitting={commandMutation.isPending || playUntilHumanMutation.isPending}
                lastPreviewResult={lastPreviewResult}
                lastCommandResult={lastCommandResult}
                operationError={previewMutation.error ?? commandMutation.error}
                onPreview={(actionType, parameters) => previewMutation.mutate({ actionType, parameters })}
                onSubmit={(actionType, parameters) => commandMutation.mutate({ actionType, parameters })}
              />
            )}
            {activeTab === "bot" && (
              <BotScreen
                state={state}
                lastBotSummary={lastBotSummary ?? state.lastBotTurnSummary}
                isBotTurnLoading={playBotTurnMutation.isPending}
                isBotUntilLoading={playUntilHumanMutation.isPending}
                operationError={playBotTurnMutation.error ?? playUntilHumanMutation.error}
                onPlayBotTurn={() => playBotTurnMutation.mutate()}
                onPlayBotUntilHuman={() => playUntilHumanMutation.mutate()}
              />
            )}
            {activeTab === "log" && <LogScreen state={state} />}
            {activeTab === "setup" && (
              <SetupScreen
                state={state}
                saveFileName={saveFileName}
                setSaveFileName={setSaveFileName}
                isSaving={saveMutation.isPending}
                isLoading={loadMutation.isPending}
                isResetting={resetMutation.isPending}
                isApplyingSetup={setupMutation.isPending}
                onApplySetup={(payload) => setupMutation.mutate(payload)}
                onSave={() => saveMutation.mutate()}
                onLoad={() => loadMutation.mutate()}
                onReset={() => resetMutation.mutate()}
                operationError={setupMutation.error ?? saveMutation.error ?? loadMutation.error ?? resetMutation.error}
              />
            )}
          </>
        )}
      </main>
      <BottomNav activeTab={activeTab} setActiveTab={setActiveTab} />
    </div>
  );
}
