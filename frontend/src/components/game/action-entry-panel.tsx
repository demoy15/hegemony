import { useEffect, useMemo, useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { useUiStore } from "@/store/use-ui-store";
import { Banknote, BriefcaseBusiness, Gavel, HandCoins, PackageOpen, Scale, Settings2, Vote } from "lucide-react";
import type {
  ActionType,
  CommandResponse,
  ComposerMetadata,
  GameState,
  LegalMove,
  PolicyId,
  PreviewActionResponse,
} from "@/types/game";

interface ActionEntryPanelProps {
  state: GameState;
  composerMetadata: ComposerMetadata;
  legalMoves: LegalMove[];
  isSubmitting: boolean;
  isPreviewing: boolean;
  onPreview: (actionType: ActionType, parameters: Record<string, unknown>, optionalModifier?: string, optionalCardReference?: string) => void;
  onSubmit: (actionType: ActionType, parameters: Record<string, unknown>) => void;
  lastCommandResult?: CommandResponse;
  lastPreviewResult?: PreviewActionResponse;
}

interface AssignmentForm {
  workerId: string;
  targetType: "ENTERPRISE_SLOT" | "UNION";
  targetId: string;
}

interface PurchaseForm {
  supplierType: "CAPITALIST" | "MIDDLE_CLASS" | "STATE" | "EXTERNAL_MARKET";
  supplierPlayerId: string;
  quantity: string;
}

interface CapitalistActionDefinition {
  id: string;
  title: string;
  kind: "basic" | "free";
  description: string;
  actionType?: ActionType;
}

const CAPITALIST_ACTIONS: CapitalistActionDefinition[] = [
  {
    id: "propose-bill",
    title: "Внести законопроект",
    kind: "basic",
    description: "Предложить сдвиг политического курса на соседний уровень.",
    actionType: "PROPOSE_BILL",
  },
  {
    id: "build-enterprise",
    title: "Построить предприятие",
    kind: "basic",
    description: "Построить MVP-предприятие за 20 из капитала.",
    actionType: "BUILD_ENTERPRISE",
  },
  {
    id: "sell-enterprise",
    title: "Продать предприятие",
    kind: "basic",
    description: "Продать первое доступное предприятие капиталиста.",
    actionType: "SELL_ENTERPRISE",
  },
  {
    id: "sell-external-market",
    title: "Продать на внешнем рынке",
    kind: "basic",
    description: "Продать накопленные товары по MVP-экспортной цене.",
    actionType: "SELL_ON_EXTERNAL_MARKET",
  },
  {
    id: "business-deal",
    title: "Заключить сделку",
    kind: "basic",
    description: "Сдать 1 food/luxury из склада за выплату.",
    actionType: "MAKE_BUSINESS_DEAL",
  },
  {
    id: "lobby",
    title: "Лоббировать интересы",
    kind: "basic",
    description: "Заплатить 30 из капитала и получить 3 влияния.",
    actionType: "LOBBY_INTERESTS",
  },
  {
    id: "political-pressure",
    title: "Политическое давление",
    kind: "basic",
    description: "Добавить 3 кубика капиталиста в мешок голосования.",
    actionType: "ADD_VOTING_CUBES",
  },
  {
    id: "change-prices",
    title: "Изменить цены",
    kind: "free",
    description: "Свободно поднять MVP-цены на 1.",
    actionType: "CHANGE_PRICES",
  },
  {
    id: "change-wages",
    title: "Изменить зарплаты",
    kind: "free",
    description: "Свободно выставить зарплаты предприятий на уровень 2.",
    actionType: "CHANGE_WAGES",
  },
  {
    id: "pay-bonus",
    title: "Выплатить бонус",
    kind: "free",
    description: "Заплатить 5 работнику на предприятии капиталиста.",
    actionType: "PAY_BONUS",
  },
  {
    id: "buy-storage",
    title: "Купить хранилище",
    kind: "free",
    description: "Заплатить 20 за MVP-хранилище food.",
    actionType: "BUY_STORAGE",
  },
  {
    id: "take-benefits",
    title: "Получить льготы",
    kind: "free",
    description: "В игре вчетвером получить 1 food и 1 влияние.",
    actionType: "TAKE_STATE_BENEFITS",
  },
  {
    id: "repay-loan",
    title: "Погасить заем",
    kind: "free",
    description: "Заплатить 50 и погасить MVP-заем.",
    actionType: "REPAY_LOAN",
  },
];

const CAPITALIST_ACTION_ICON: Record<string, typeof Gavel> = {
  "propose-bill": Gavel,
  "build-enterprise": BriefcaseBusiness,
  "sell-enterprise": HandCoins,
  "sell-external-market": PackageOpen,
  "business-deal": HandCoins,
  lobby: Banknote,
  "political-pressure": Vote,
  "change-prices": Settings2,
  "change-wages": Settings2,
  "pay-bonus": HandCoins,
  "buy-storage": PackageOpen,
  "take-benefits": Scale,
  "repay-loan": Banknote,
};

const MIDDLE_CLASS_ACTIONS: CapitalistActionDefinition[] = [
  {
    id: "propose-bill",
    title: "Внести законопроект",
    kind: "basic",
    description: "Выбрать политику и поместить фишку на соседний курс; после этого можно потратить влияние на внеочередное голосование.",
    actionType: "PROPOSE_BILL",
  },
  {
    id: "assign-workers",
    title: "Назначить работников",
    kind: "basic",
    description: "Трудоустроить до 3 работников на свободные места предприятий или перевести незаконтрактованных работников.",
    actionType: "ASSIGN_WORKERS",
  },
  {
    id: "build-enterprise",
    title: "Построить предприятие",
    kind: "basic",
    description: "Построить предприятие среднего класса, если хватает денег, лимита и доступных работников для MVP-среза.",
    actionType: "BUILD_ENTERPRISE",
  },
  {
    id: "sell-enterprise",
    title: "Продать предприятие",
    kind: "basic",
    description: "Сбросить предприятие и вернуть работников на безработицу; полная команда еще не подключена для среднего класса.",
    actionType: "SELL_ENTERPRISE",
  },
  {
    id: "sell-external-market",
    title: "Экспорт",
    kind: "basic",
    description: "Продать ресурсы из хранилищ по текущей карте экспорта и получить ПО за операцию.",
    actionType: "SELL_ON_EXTERNAL_MARKET",
  },
  {
    id: "buy-goods-services",
    title: "Купить товары и услуги",
    kind: "basic",
    description: "Купить один вид ресурса не более чем у 2 поставщиков, включая бесплатное перемещение своих ресурсов.",
    actionType: "BUY_GOODS_AND_SERVICES",
  },
  {
    id: "extra-shift",
    title: "Дополнительная смена",
    kind: "basic",
    description: "Произвести ресурсы на своем предприятии и связать работников трудовым договором; команда еще не в MVP.",
  },
  {
    id: "political-pressure",
    title: "Политическое давление",
    kind: "basic",
    description: "Добавить 3 кубика среднего класса в мешочек для голосования.",
    actionType: "ADD_VOTING_CUBES",
  },
  {
    id: "consume-healthcare",
    title: "Получить медпомощь",
    kind: "free",
    description: "Потратить медицину по населению, получить благосостояние, 2 ПО и нового неквалифицированного работника.",
    actionType: "CONSUME_HEALTHCARE",
  },
  {
    id: "consume-education",
    title: "Получить образование",
    kind: "free",
    description: "Потратить образование по населению, поднять благосостояние и обучить выбранного работника.",
    actionType: "CONSUME_EDUCATION",
  },
  {
    id: "consume-luxury",
    title: "Использовать роскошь",
    kind: "free",
    description: "Потратить предметы роскоши по населению и получить +1 благосостояние.",
    actionType: "CONSUME_LUXURY",
  },
  {
    id: "change-prices",
    title: "Изменить цены",
    kind: "free",
    description: "Переместить маркеры стоимости товаров и услуг; для среднего класса команда еще не подключена.",
    actionType: "CHANGE_PRICES",
  },
  {
    id: "change-wages",
    title: "Изменить зарплаты",
    kind: "free",
    description: "Изменить оплату наемным рабочим; для среднего класса команда еще не подключена.",
    actionType: "CHANGE_WAGES",
  },
  {
    id: "replace-workers",
    title: "Заменить работников",
    kind: "free",
    description: "Поменять специалистов на безработных с переносом статуса ТД; команда еще не в MVP.",
  },
  {
    id: "take-benefits",
    title: "Получить льготы",
    kind: "free",
    description: "Забрать ресурсы или бонусы с клетки государственных льгот в партии на 4 игроков.",
    actionType: "TAKE_STATE_BENEFITS",
  },
  {
    id: "repay-loan",
    title: "Погасить заем",
    kind: "free",
    description: "Заплатить 50 монет и сбросить карту займа.",
    actionType: "REPAY_LOAN",
  },
];

const MIDDLE_CLASS_ACTION_ICON: Record<string, typeof Gavel> = {
  "propose-bill": Gavel,
  "assign-workers": Settings2,
  "build-enterprise": BriefcaseBusiness,
  "sell-enterprise": HandCoins,
  "sell-external-market": PackageOpen,
  "buy-goods-services": HandCoins,
  "extra-shift": BriefcaseBusiness,
  "political-pressure": Vote,
  "consume-healthcare": Scale,
  "consume-education": Scale,
  "consume-luxury": Scale,
  "change-prices": Settings2,
  "change-wages": Settings2,
  "replace-workers": Settings2,
  "take-benefits": Scale,
  "repay-loan": Banknote,
};

const FORM_ACTIONS = new Set<ActionType>([
  "PROPOSE_BILL",
  "CALL_EXTRAORDINARY_VOTE",
  "ASSIGN_WORKERS",
  "BUY_GOODS_AND_SERVICES",
  "CONSUME_EDUCATION",
  "BUILD_ENTERPRISE",
  "SELL_ENTERPRISE",
  "CHANGE_PRICES",
  "CHANGE_WAGES",
]);

const STATE_ACTIONS: CapitalistActionDefinition[] = [
  {
    id: "propose-bill",
    title: "Внести законопроект",
    kind: "basic",
    description: "Предложить изменение политического курса на соседнее деление.",
    actionType: "PROPOSE_BILL",
  },
  {
    id: "respond-event",
    title: "Отреагировать на событие",
    kind: "basic",
    description: "Выполнить MVP-реакцию на активное событие: поддержать класс с низкой легитимностью и получить награду.",
    actionType: "RESPOND_TO_EVENT",
  },
  {
    id: "sell-external-market",
    title: "Продать на внешнем рынке",
    kind: "basic",
    description: "Продать экспортируемые товары государства по текущей карте экспорта.",
    actionType: "SELL_ON_EXTERNAL_MARKET",
  },
  {
    id: "meet-deputies",
    title: "Встретиться с депутатами",
    kind: "basic",
    description: "Передать выбранному классу 2 влияния и повысить его легитимность на 1.",
    actionType: "MEET_DEPUTIES",
  },
  {
    id: "extra-tax",
    title: "Ввести дополнительный налог",
    kind: "basic",
    description: "Взять по 10 монет у соперников и снизить две самые низкие легитимности на 1.",
    actionType: "INTRODUCE_EXTRA_TAX",
  },
  {
    id: "campaign",
    title: "Провести кампанию",
    kind: "basic",
    description: "Превратить до 3 влияния СМИ из госуслуг в личное влияние государства.",
    actionType: "RUN_CAMPAIGN",
  },
  {
    id: "change-wages",
    title: "Изменить зарплаты",
    kind: "free",
    description: "Изменить оплату труда на государственных предприятиях с учетом МРОТ.",
    actionType: "CHANGE_WAGES",
  },
  {
    id: "repay-loan",
    title: "Погасить заём",
    kind: "free",
    description: "Заплатить 50 из казначейства и сбросить карту займа государства.",
    actionType: "REPAY_LOAN",
  },
  {
    id: "call-extraordinary-vote",
    title: "Внеочередное голосование",
    kind: "free",
    description: "Потратить 1 личное влияние сразу после законопроекта и открыть внеочередное голосование.",
    actionType: "CALL_EXTRAORDINARY_VOTE",
  },
];

const STATE_ACTION_ICON: Record<string, typeof Gavel> = {
  "propose-bill": Gavel,
  "respond-event": Scale,
  "sell-external-market": PackageOpen,
  "meet-deputies": Vote,
  "extra-tax": Banknote,
  campaign: Vote,
  "change-wages": Settings2,
  "repay-loan": Banknote,
  "call-extraordinary-vote": Vote,
};

export function ActionEntryPanel({
  state,
  composerMetadata,
  legalMoves,
  isSubmitting,
  isPreviewing,
  onPreview,
  onSubmit,
  lastCommandResult,
  lastPreviewResult,
}: ActionEntryPanelProps) {
  const selectedAction = useUiStore((s) => s.selectedAction);
  const setSelectedAction = useUiStore((s) => s.setSelectedAction);
  const guidedActions = useMemo(() => composerMetadata.actionTemplates.map((t) => t.actionType), [composerMetadata.actionTemplates]);

  const currentPlayer = state.players[state.turnOrder.currentPlayerIndex];
  const [actorPlayerId, setActorPlayerId] = useState(currentPlayer?.playerId ?? "");
  const [policyId, setPolicyId] = useState<PolicyId>(state.policies[0]?.id ?? "POLICY_1_FISCAL");
  const [targetCourse, setTargetCourse] = useState<"A" | "B" | "C">("B");
  const [assignments, setAssignments] = useState<AssignmentForm[]>([{ workerId: "", targetType: "ENTERPRISE_SLOT", targetId: "" }]);
  const [resourceType, setResourceType] = useState<"FOOD" | "LUXURY" | "HEALTHCARE" | "EDUCATION">("FOOD");
  const [purchases, setPurchases] = useState<PurchaseForm[]>([{ supplierType: "CAPITALIST", supplierPlayerId: "", quantity: "1" }]);
  const [educationWorkerId, setEducationWorkerId] = useState("");
  const [educationTargetColor, setEducationTargetColor] = useState<"GREEN" | "BLUE" | "RED" | "WHITE" | "ORANGE" | "PURPLE">("WHITE");
  const [selectedLegalMoveId, setSelectedLegalMoveId] = useState("");
  const [hireWorkerClass, setHireWorkerClass] = useState(false);
  const [wageLevel, setWageLevel] = useState("2");
  const [pricePatch, setPricePatch] = useState<Record<string, string>>({
    food: "",
    luxury: "",
    healthcare: "",
    education: "",
  });
  const [optionalModifier, setOptionalModifier] = useState("");
  const [optionalCardReference, setOptionalCardReference] = useState("");

  useEffect(() => {
    setActorPlayerId(composerMetadata.actorPlayerId || currentPlayer?.playerId || "");
  }, [composerMetadata.actorPlayerId, currentPlayer?.playerId]);

  useEffect(() => {
    if (!guidedActions.includes(selectedAction) && guidedActions.length > 0) {
      setSelectedAction(guidedActions[0]);
    }
  }, [guidedActions, selectedAction, setSelectedAction]);

  const legalMoveTypes = useMemo(() => new Set(legalMoves.map((m) => m.actionType)), [legalMoves]);
  const selectedActionLegalMoves = useMemo(() => legalMoves.filter((move) => move.actionType === selectedAction), [legalMoves, selectedAction]);
  const legalMovesForAction = (actionType?: ActionType) => {
    if (!actionType) {
      return [];
    }
    return legalMoves.filter((move) => move.actionType === actionType);
  };
  const primaryLegalMoveForAction = (actionType?: ActionType) =>
    (actionType === selectedAction ? selectedActionLegalMoves.find((move) => move.id === selectedLegalMoveId) : undefined) ??
    legalMovesForAction(actionType).find((move) => Object.keys(move.template ?? {}).length > 0) ??
    legalMovesForAction(actionType)[0];
  const legalTemplateForAction = (actionType: ActionType): Record<string, unknown> => {
    const move = primaryLegalMoveForAction(actionType);
    return { ...(move?.template ?? {}), actorPlayerId: (move?.template?.actorPlayerId as string | undefined) ?? actorPlayerId };
  };
  const selectedTemplate = useMemo(
    () => composerMetadata.actionTemplates.find((template) => template.actionType === selectedAction),
    [composerMetadata.actionTemplates, selectedAction],
  );
  const isChosenActionLegal = legalMoveTypes.has(selectedAction);
  const isHumanCapitalistTurn =
    currentPlayer?.classType === "CAPITALIST" && currentPlayer.controlMode === "HUMAN";
  const isHumanMiddleClassTurn =
    currentPlayer?.classType === "MIDDLE_CLASS" && currentPlayer.controlMode === "HUMAN";
  const isHumanStateTurn =
    currentPlayer?.classType === "STATE" && currentPlayer.controlMode === "HUMAN";

  useEffect(() => {
    if (selectedActionLegalMoves.length === 0) {
      setSelectedLegalMoveId("");
      return;
    }
    if (!selectedActionLegalMoves.some((move) => move.id === selectedLegalMoveId)) {
      setSelectedLegalMoveId(selectedActionLegalMoves[0].id);
    }
  }, [selectedActionLegalMoves, selectedLegalMoveId]);

  const workerOptions = useMemo(
    () =>
      state.workers
        .filter((worker) => {
          const actor = state.players.find((player) => player.playerId === actorPlayerId);
          return !actor || worker.classType === actor.classType;
        })
        .map((worker) => ({
          id: worker.id,
          label: `${worker.id} (${worker.classType}, ${worker.qualificationType}, ${worker.location}${worker.tiedContract ? ", tied" : ""})`,
        })),
    [state.workers, state.players, actorPlayerId],
  );

  const slotOptions = useMemo(
    () =>
      state.enterprises.flatMap((enterprise) =>
        enterprise.slots.map((slot) => ({
          id: `${enterprise.id}:${slot.id}`,
          label: `${enterprise.id}:${slot.id} (${slot.requiredQualification}${slot.requiredSector ? `/${slot.requiredSector}` : ""})`,
        })),
      ),
    [state.enterprises],
  );

  const updateAssignment = (index: number, patch: Partial<AssignmentForm>) => {
    setAssignments((prev) => prev.map((entry, idx) => (idx === index ? { ...entry, ...patch } : entry)));
  };

  const addAssignment = () => setAssignments((prev) => (prev.length >= 3 ? prev : [...prev, { workerId: "", targetType: "ENTERPRISE_SLOT", targetId: "" }]));
  const removeAssignment = (index: number) => setAssignments((prev) => (prev.length <= 1 ? prev : prev.filter((_, idx) => idx !== index)));

  const updatePurchase = (index: number, patch: Partial<PurchaseForm>) => {
    setPurchases((prev) => prev.map((entry, idx) => (idx === index ? { ...entry, ...patch } : entry)));
  };

  const addPurchase = () => setPurchases((prev) => (prev.length >= 2 ? prev : [...prev, { supplierType: "STATE", supplierPlayerId: "", quantity: "1" }]));
  const removePurchase = (index: number) => setPurchases((prev) => (prev.length <= 1 ? prev : prev.filter((_, idx) => idx !== index)));

  const buildParameters = (): Record<string, unknown> => {
    if (selectedAction === "PROPOSE_BILL") {
      return { actorPlayerId, policyId, targetCourse };
    }
    if (selectedAction === "ASSIGN_WORKERS") {
      return {
        actorPlayerId,
        assignments: assignments
          .filter((entry) => entry.workerId && entry.targetId)
          .map((entry) => ({ workerId: entry.workerId, targetType: entry.targetType, targetId: entry.targetId })),
      };
    }
    if (selectedAction === "BUY_GOODS_AND_SERVICES") {
      return {
        actorPlayerId,
        resourceType,
        purchases: purchases.map((entry) => ({
          supplierType: entry.supplierType,
          supplierPlayerId: entry.supplierPlayerId || undefined,
          quantity: Number(entry.quantity),
        })),
      };
    }
    if (selectedAction === "CALL_EXTRAORDINARY_VOTE") {
      return { actorPlayerId, policyId };
    }
    if (selectedAction === "CONSUME_EDUCATION") {
      return { actorPlayerId, workerId: educationWorkerId, targetColor: educationTargetColor };
    }
    if (selectedAction === "BUILD_ENTERPRISE") {
      const params = legalTemplateForAction(selectedAction);
      if (params.workerClassSlot) {
        params.hireWorkerClass = hireWorkerClass;
        params.wageLevel = Number(wageLevel);
      }
      return params;
    }
    if (selectedAction === "CHANGE_WAGES") {
      return { ...legalTemplateForAction(selectedAction), wageLevel: Number(wageLevel) };
    }
    if (selectedAction === "CHANGE_PRICES") {
      const prices = Object.fromEntries(
        Object.entries(pricePatch)
          .filter(([, value]) => value !== "")
          .map(([resourceId, value]) => [resourceId, Number(value)]),
      );
      return Object.keys(prices).length > 0 ? { ...legalTemplateForAction(selectedAction), prices } : legalTemplateForAction(selectedAction);
    }
    return legalTemplateForAction(selectedAction);
  };

  const submitPreview = () => onPreview(selectedAction, buildParameters(), optionalModifier, optionalCardReference);
  const submitCommand = () => onSubmit(selectedAction, buildParameters());
  const submitQuickAction = (actionType: ActionType) => onSubmit(actionType, legalTemplateForAction(actionType));
  const moveOptionLabel = (move: LegalMove) => {
    const target = move.template.enterpriseId ?? move.template.dealId ?? move.template.resourceType;
    const cost = move.template.cost;
    const parts = [target ? String(target) : move.id, cost ? `cost ${cost}` : "", move.summary].filter(Boolean);
    return parts.join(" | ");
  };
  const selectClassAction = (action: CapitalistActionDefinition) => {
    if (!action.actionType) {
      return;
    }
    if (!FORM_ACTIONS.has(action.actionType)) {
      submitQuickAction(action.actionType);
      return;
    }
    setSelectedAction(action.actionType);
  };

  const renderClassActionButton = (action: CapitalistActionDefinition, iconMap: Record<string, typeof Gavel>) => {
    const Icon = iconMap[action.id] ?? Settings2;
    const legal = action.actionType ? legalMoveTypes.has(action.actionType) : false;
    const selected = action.actionType === selectedAction;
    const primaryMove = primaryLegalMoveForAction(action.actionType);
    const targetId = primaryMove?.template?.enterpriseId ?? primaryMove?.template?.dealId ?? primaryMove?.template?.resourceType ?? primaryMove?.template?.targetClass;
    const connectedLabel = `Connected to real command${targetId ? `: ${targetId}` : ""}.`;
    return (
      <button
        key={action.id}
        type="button"
        onClick={() => selectClassAction(action)}
        disabled={!legal || isSubmitting}
        title={legal ? action.description : `${action.description} ${action.actionType ? "Сейчас ход нелегален по бэкенду." : "Команда бэкенда еще не установлена."}`}
        className={[
          "grid min-h-[96px] grid-cols-[2.25rem_1fr] gap-3 rounded-lg border p-3 text-left transition",
          legal ? "border-emerald-500/45 bg-emerald-500/10 hover:bg-emerald-500/15" : "border-border/70 bg-muted/35 opacity-70",
          selected ? "ring-2 ring-primary/70" : "",
        ].join(" ")}
      >
        <span className="flex h-9 w-9 items-center justify-center rounded-md bg-background/70 text-foreground">
          <Icon className="h-4 w-4" aria-hidden="true" />
        </span>
        <span className="min-w-0 space-y-1">
          <span className="block text-sm font-semibold leading-snug">{action.title}</span>
          <span className="block text-xs leading-relaxed text-muted-foreground">{legal ? connectedLabel : action.description}</span>
          <span className="block text-[11px] uppercase text-muted-foreground">
            {legal ? "Доступно сейчас" : action.actionType ? "Недоступно по правилам" : "Еще не в MVP"}
          </span>
        </span>
      </button>
    );
  };

  const selectedLegalMove = primaryLegalMoveForAction(selectedAction);
  const selectedEnterpriseId = selectedLegalMove?.template.enterpriseId ? String(selectedLegalMove.template.enterpriseId) : "";
  const selectedEnterprise =
    state.middleClassEnterpriseMarket?.find((enterprise) => enterprise.id === selectedEnterpriseId) ??
    state.capitalistEnterpriseMarket?.find((enterprise) => enterprise.id === selectedEnterpriseId) ??
    state.enterprises.find((enterprise) => enterprise.id === selectedEnterpriseId);
  const selectedMoveHasHiredSlot = Boolean(selectedLegalMove?.template.workerClassSlot);

  return (
    <Card>
      <CardHeader>
        <CardTitle>Human Action Composer</CardTitle>
        <CardDescription>Guided flow: choose action, preview delta, then confirm.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {isHumanCapitalistTurn && (
          <div className="space-y-3 rounded-lg border border-border/80 p-3">
            <div className="flex flex-wrap items-center gap-2">
              <p className="text-xs uppercase tracking-wide text-muted-foreground">Действия капиталиста</p>
              <Badge tone="positive">Human turn</Badge>
              <Badge tone="neutral">{actorPlayerId}</Badge>
            </div>
            <div className="grid gap-2 md:grid-cols-2 xl:grid-cols-3">
              {CAPITALIST_ACTIONS.filter((action) => action.kind === "basic").map((action) => renderClassActionButton(action, CAPITALIST_ACTION_ICON))}
            </div>
            <div className="grid gap-2 md:grid-cols-2 xl:grid-cols-3">
              {CAPITALIST_ACTIONS.filter((action) => action.kind === "free").map((action) => renderClassActionButton(action, CAPITALIST_ACTION_ICON))}
            </div>
          </div>
        )}

        {isHumanMiddleClassTurn && (
          <div className="space-y-3 rounded-lg border border-border/80 p-3">
            <div className="flex flex-wrap items-center gap-2">
              <p className="text-xs uppercase tracking-wide text-muted-foreground">Действия среднего класса</p>
              <Badge tone="positive">Human turn</Badge>
              <Badge tone="neutral">{actorPlayerId}</Badge>
            </div>
            <div className="grid gap-2 md:grid-cols-2 xl:grid-cols-3">
              {MIDDLE_CLASS_ACTIONS.filter((action) => action.kind === "basic").map((action) => renderClassActionButton(action, MIDDLE_CLASS_ACTION_ICON))}
            </div>
            <div className="grid gap-2 md:grid-cols-2 xl:grid-cols-3">
              {MIDDLE_CLASS_ACTIONS.filter((action) => action.kind === "free").map((action) => renderClassActionButton(action, MIDDLE_CLASS_ACTION_ICON))}
            </div>
          </div>
        )}

        {isHumanStateTurn && (
          <div className="space-y-3 rounded-lg border border-border/80 p-3">
            <div className="flex flex-wrap items-center gap-2">
              <p className="text-xs uppercase tracking-wide text-muted-foreground">Действия государства</p>
              <Badge tone="positive">Human turn</Badge>
              <Badge tone="neutral">{actorPlayerId}</Badge>
            </div>
            <div className="grid gap-2 md:grid-cols-2 xl:grid-cols-3">
              {STATE_ACTIONS.filter((action) => action.kind === "basic").map((action) => renderClassActionButton(action, STATE_ACTION_ICON))}
            </div>
            <div className="grid gap-2 md:grid-cols-2 xl:grid-cols-3">
              {STATE_ACTIONS.filter((action) => action.kind === "free").map((action) => renderClassActionButton(action, STATE_ACTION_ICON))}
            </div>
          </div>
        )}

        <div className="space-y-2">
          <label className="text-xs uppercase tracking-wide text-muted-foreground">Action</label>
          <Select value={selectedAction} onChange={(e) => setSelectedAction(e.target.value as ActionType)}>
            {composerMetadata.actionTemplates.map((template) => (
              <option key={template.actionType} value={template.actionType}>
                {template.actionType}
              </option>
            ))}
          </Select>
          <div className="flex flex-wrap gap-2">
            {isChosenActionLegal ? <Badge tone="positive">Legal now</Badge> : <Badge tone="warning">Currently illegal</Badge>}
            {selectedTemplate?.futureModifierSlot ? <Badge tone="positive">Modifier slot ready</Badge> : null}
          </div>
          <p className="text-xs text-muted-foreground">{selectedTemplate?.summary}</p>
        </div>

        <div className="space-y-2">
          <label className="text-xs uppercase tracking-wide text-muted-foreground">Actor</label>
          <Select value={actorPlayerId} onChange={(e) => setActorPlayerId(e.target.value)}>
            {state.players.map((player) => (
              <option key={player.playerId} value={player.playerId}>
                {player.playerId} ({player.classType}, {player.controlMode})
              </option>
            ))}
          </Select>
        </div>

        {["BUILD_ENTERPRISE", "SELL_ENTERPRISE", "CHANGE_WAGES", "MEET_DEPUTIES"].includes(selectedAction) && selectedActionLegalMoves.length > 0 && (
          <div className="space-y-2 rounded-xl border border-border/80 p-3">
            <label className="text-xs uppercase tracking-wide text-muted-foreground">Legal target</label>
            <Select value={selectedLegalMoveId} onChange={(e) => setSelectedLegalMoveId(e.target.value)}>
              {selectedActionLegalMoves.map((move) => (
                <option key={move.id} value={move.id}>
                  {moveOptionLabel(move)}
                </option>
              ))}
            </Select>
            {selectedEnterprise && (
              <div className="grid gap-2 text-xs text-muted-foreground sm:grid-cols-3">
                <span>{selectedEnterprise.name ?? selectedEnterprise.id}</span>
                <span>cost {String(selectedEnterprise.cost ?? selectedLegalMove?.template.cost ?? "-")}</span>
                <span>wage {selectedEnterprise.wageLevel}</span>
              </div>
            )}
          </div>
        )}

        {selectedAction === "BUILD_ENTERPRISE" && selectedMoveHasHiredSlot && (
          <div className="grid gap-3 rounded-xl border border-border/80 p-3 sm:grid-cols-2">
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={hireWorkerClass} onChange={(e) => setHireWorkerClass(e.target.checked)} />
              Hire worker-class employee
            </label>
            <div className="space-y-2">
              <label className="text-xs uppercase tracking-wide text-muted-foreground">Wage level</label>
              <Select value={wageLevel} onChange={(e) => setWageLevel(e.target.value)}>
                <option value="1">1</option>
                <option value="2">2</option>
                <option value="3">3</option>
              </Select>
            </div>
          </div>
        )}

        {selectedAction === "CHANGE_WAGES" && (
          <div className="space-y-2 rounded-xl border border-border/80 p-3">
            <label className="text-xs uppercase tracking-wide text-muted-foreground">Wage level</label>
            <Select value={wageLevel} onChange={(e) => setWageLevel(e.target.value)}>
              <option value="1">1</option>
              <option value="2">2</option>
              <option value="3">3</option>
            </Select>
          </div>
        )}

        {selectedAction === "CHANGE_PRICES" && (
          <div className="grid gap-2 rounded-xl border border-border/80 p-3 sm:grid-cols-4">
            {Object.entries(pricePatch).map(([resourceId, value]) => (
              <div key={resourceId} className="space-y-2">
                <label className="text-xs uppercase tracking-wide text-muted-foreground">{resourceId}</label>
                <Input
                  value={value}
                  onChange={(e) => setPricePatch((prev) => ({ ...prev, [resourceId]: e.target.value }))}
                  type="number"
                  min={0}
                  placeholder={String(currentPlayer?.prices?.[resourceId] ?? "")}
                />
              </div>
            ))}
          </div>
        )}

        {selectedAction === "PROPOSE_BILL" && (
          <>
            <div className="space-y-2">
              <label className="text-xs uppercase tracking-wide text-muted-foreground">Policy</label>
              <Select value={policyId} onChange={(e) => setPolicyId(e.target.value as PolicyId)}>
                {state.policies.map((policy) => (
                  <option key={policy.id} value={policy.id}>
                    {policy.id} ({policy.currentCourse})
                  </option>
                ))}
              </Select>
            </div>
            <div className="space-y-2">
              <label className="text-xs uppercase tracking-wide text-muted-foreground">Target Course</label>
              <Select value={targetCourse} onChange={(e) => setTargetCourse(e.target.value as "A" | "B" | "C")}>
                <option value="A">A</option>
                <option value="B">B</option>
                <option value="C">C</option>
              </Select>
            </div>
          </>
        )}

        {selectedAction === "CALL_EXTRAORDINARY_VOTE" && (
          <div className="space-y-2">
            <label className="text-xs uppercase tracking-wide text-muted-foreground">Policy</label>
            <Select value={policyId} onChange={(e) => setPolicyId(e.target.value as PolicyId)}>
              {state.policies.map((policy) => (
                <option key={policy.id} value={policy.id}>
                  {policy.id} ({policy.currentCourse})
                </option>
              ))}
            </Select>
          </div>
        )}

        {selectedAction === "ASSIGN_WORKERS" && (
          <div className="space-y-3 rounded-xl border border-border/80 p-3">
            {assignments.map((entry, index) => (
              <div key={`assignment-${index}`} className="grid gap-2 sm:grid-cols-3">
                <Select value={entry.workerId} onChange={(e) => updateAssignment(index, { workerId: e.target.value })}>
                  <option value="">Worker</option>
                  {workerOptions.map((worker) => (
                    <option key={worker.id} value={worker.id}>
                      {worker.label}
                    </option>
                  ))}
                </Select>
                <Select value={entry.targetType} onChange={(e) => updateAssignment(index, { targetType: e.target.value as "ENTERPRISE_SLOT" | "UNION" })}>
                  <option value="ENTERPRISE_SLOT">ENTERPRISE_SLOT</option>
                  <option value="UNION">UNION</option>
                </Select>
                <div className="flex gap-2">
                  <Select value={entry.targetId} onChange={(e) => updateAssignment(index, { targetId: e.target.value })}>
                    <option value="">Target</option>
                    {slotOptions.map((slot) => (
                      <option key={slot.id} value={slot.id}>
                        {slot.label}
                      </option>
                    ))}
                  </Select>
                  <Button type="button" variant="outline" onClick={() => removeAssignment(index)}>
                    -
                  </Button>
                </div>
              </div>
            ))}
            <Button type="button" variant="secondary" onClick={addAssignment}>
              Add assignment
            </Button>
          </div>
        )}

        {selectedAction === "BUY_GOODS_AND_SERVICES" && (
          <div className="space-y-3 rounded-xl border border-border/80 p-3">
            <div className="space-y-2">
              <label className="text-xs uppercase tracking-wide text-muted-foreground">Resource Type</label>
              <Select value={resourceType} onChange={(e) => setResourceType(e.target.value as "FOOD" | "LUXURY" | "HEALTHCARE" | "EDUCATION")}>
                <option value="FOOD">FOOD</option>
                <option value="LUXURY">LUXURY</option>
                <option value="HEALTHCARE">HEALTHCARE</option>
                <option value="EDUCATION">EDUCATION</option>
              </Select>
            </div>
            {purchases.map((entry, index) => (
              <div key={`purchase-${index}`} className="grid gap-2 sm:grid-cols-4">
                <Select value={entry.supplierType} onChange={(e) => updatePurchase(index, { supplierType: e.target.value as PurchaseForm["supplierType"] })}>
                  <option value="CAPITALIST">CAPITALIST</option>
                  <option value="MIDDLE_CLASS">MIDDLE_CLASS</option>
                  <option value="STATE">STATE</option>
                  <option value="EXTERNAL_MARKET">EXTERNAL_MARKET</option>
                </Select>
                <Input value={entry.supplierPlayerId} onChange={(e) => updatePurchase(index, { supplierPlayerId: e.target.value })} placeholder="supplierPlayerId" />
                <Input value={entry.quantity} onChange={(e) => updatePurchase(index, { quantity: e.target.value })} type="number" min={1} />
                <Button type="button" variant="outline" onClick={() => removePurchase(index)}>
                  -
                </Button>
              </div>
            ))}
            <Button type="button" variant="secondary" onClick={addPurchase}>
              Add supplier
            </Button>
          </div>
        )}

        {selectedAction === "CONSUME_EDUCATION" && (
          <div className="grid gap-2 rounded-xl border border-border/80 p-3 sm:grid-cols-2">
            <div className="space-y-2">
              <label className="text-xs uppercase tracking-wide text-muted-foreground">Worker</label>
              <Select value={educationWorkerId} onChange={(e) => setEducationWorkerId(e.target.value)}>
                <option value="">Worker</option>
                {state.workers
                  .filter((worker) => worker.classType === currentPlayer?.classType && worker.qualificationType === "UNSKILLED")
                  .map((worker) => (
                    <option key={worker.id} value={worker.id}>
                      {worker.id} ({worker.location})
                    </option>
                  ))}
              </Select>
            </div>
            <div className="space-y-2">
              <label className="text-xs uppercase tracking-wide text-muted-foreground">Target Color</label>
              <Select value={educationTargetColor} onChange={(e) => setEducationTargetColor(e.target.value as "GREEN" | "BLUE" | "RED" | "WHITE" | "ORANGE" | "PURPLE")}>
                <option value="GREEN">GREEN</option>
                <option value="BLUE">BLUE</option>
                <option value="RED">RED</option>
                <option value="WHITE">WHITE</option>
                <option value="ORANGE">ORANGE</option>
                <option value="PURPLE">PURPLE</option>
              </Select>
            </div>
          </div>
        )}

        <div className="space-y-2 rounded-xl border border-border/80 p-3">
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Optional Modifier Slot (Future-ready)</p>
          <p className="text-xs text-muted-foreground">{composerMetadata.modifierAvailabilityNote}</p>
          <Input value={optionalModifier} onChange={(e) => setOptionalModifier(e.target.value)} placeholder="modifier id (optional)" />
          <Input value={optionalCardReference} onChange={(e) => setOptionalCardReference(e.target.value)} placeholder="card reference (optional)" />
        </div>

        <div className="grid gap-2 sm:grid-cols-2">
          <Button onClick={submitPreview} disabled={isPreviewing}>
            {isPreviewing ? "Previewing..." : "Preview"}
          </Button>
          <Button onClick={submitCommand} disabled={isSubmitting}>
            {isSubmitting ? "Submitting..." : "Confirm Action"}
          </Button>
        </div>

        {lastPreviewResult && (
          <div className="space-y-2 rounded-xl border border-border/80 p-3 text-sm">
            <p className="font-semibold">Preview Result</p>
            {!lastPreviewResult.accepted && (
              <ul className="list-disc pl-5 text-red-300">
                {lastPreviewResult.errors.map((error, idx) => (
                  <li key={`${error}-${idx}`}>{error}</li>
                ))}
              </ul>
            )}
            {lastPreviewResult.accepted && (
              <>
                <p className="text-xs text-muted-foreground">Money delta: {JSON.stringify(lastPreviewResult.delta.moneyDeltaByPlayer)}</p>
                <p className="text-xs text-muted-foreground">Resource delta: {JSON.stringify(lastPreviewResult.delta.resourceDeltaByPlayer)}</p>
                <p className="text-xs text-muted-foreground">Worker movement: {JSON.stringify(lastPreviewResult.delta.workerMovement)}</p>
              </>
            )}
            {lastPreviewResult.supportNotes.length > 0 && (
              <p className="text-xs text-amber-300">Notes: {lastPreviewResult.supportNotes.join(", ")}</p>
            )}
          </div>
        )}

        {lastCommandResult && !lastCommandResult.accepted && (
          <div className="rounded-xl border border-red-500/40 bg-red-500/10 p-3 text-sm text-red-100">
            <p className="mb-1 font-semibold">Validation errors</p>
            <ul className="list-disc space-y-1 pl-5">
              {lastCommandResult.errors.map((error, idx) => (
                <li key={`${error}-${idx}`}>{error}</li>
              ))}
            </ul>
          </div>
        )}

        <div className="rounded-xl border border-border/80 p-3">
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Explicitly unavailable now</p>
          <ul className="list-disc space-y-1 pl-5 text-xs text-muted-foreground">
            {composerMetadata.unavailableActionNotes.map((note, idx) => (
              <li key={`${note}-${idx}`}>{note}</li>
            ))}
          </ul>
        </div>
      </CardContent>
    </Card>
  );
}
