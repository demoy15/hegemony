import type { ActionType, ClassType, PolicyId, RoundPhase } from "@/types/game";

export const CLASS_LABEL: Record<ClassType, string> = {
  WORKER: "Рабочие",
  MIDDLE_CLASS: "Средний класс",
  CAPITALIST: "Капиталисты",
  STATE: "Государство",
};

export const CLASS_SHORT: Record<ClassType, string> = {
  WORKER: "Раб.",
  MIDDLE_CLASS: "Сред.",
  CAPITALIST: "Кап.",
  STATE: "Гос.",
};

export const POLICY_LABEL: Record<PolicyId, string> = {
  POLICY_1_FISCAL: "Фискальная",
  POLICY_2_LABOR_MARKET: "Рынок труда",
  POLICY_3_TAXATION: "Налоги",
  POLICY_4_HEALTHCARE_AND_BENEFITS: "Здравоохранение",
  POLICY_5_EDUCATION: "Образование",
  POLICY_6_FOREIGN_TRADE: "Внешняя торговля",
  POLICY_7_IMMIGRATION: "Миграция",
};

export const ACTION_LABEL: Record<ActionType, string> = {
  ADVANCE_TO_VOTING: "К голосованию",
  ADVANCE_TO_PRODUCTION: "К производству",
  RESOLVE_PRODUCTION_PHASE: "Завершить производство",
  ADVANCE_TO_SCORING: "К подсчету",
  RESOLVE_SCORING_PHASE: "Завершить подсчет",
  ADVANCE_TO_NEXT_ROUND: "Следующий раунд",
  RESOLVE_PREPARATION_PHASE: "Завершить подготовку",
  ADVANCE_GAME_FLOW: "Следующий шаг",
  ADVANCE_ROUND: "Продвинуть раунд",
  DECLARE_VOTE_STANCE: "Позиция",
  DRAW_VOTING_CUBES: "Достать кубики",
  COMMIT_VOTE_INFLUENCE: "Вложить влияние",
  PROPOSE_BILL: "Предложить закон",
  ADD_VOTING_CUBES: "Добавить кубики",
  CALL_EXTRAORDINARY_VOTE: "Внеочередное голосование",
  BUILD_ENTERPRISE: "Построить предприятие",
  SELL_ENTERPRISE: "Продать предприятие",
  SELL_ON_EXTERNAL_MARKET: "Внешний рынок",
  MAKE_BUSINESS_DEAL: "Сделка",
  LOBBY_INTERESTS: "Лоббировать",
  CHANGE_PRICES: "Цены",
  CHANGE_WAGES: "Зарплаты",
  PAY_BONUS: "Бонус",
  BUY_STORAGE: "Хранилище",
  TAKE_STATE_BENEFITS: "Льготы",
  REPAY_LOAN: "Погасить заем",
  RESPOND_TO_EVENT: "Событие",
  MEET_DEPUTIES: "Депутаты",
  INTRODUCE_EXTRA_TAX: "Доп. налог",
  RUN_CAMPAIGN: "Кампания",
  ASSIGN_WORKERS: "Назначить рабочих",
  PLACE_STRIKES: "Забастовка",
  PLACE_DEMONSTRATION: "Демонстрация",
  BUY_GOODS_AND_SERVICES: "Купить товары",
  CONSUME_HEALTHCARE: "Медицина",
  CONSUME_EDUCATION: "Образование",
  CONSUME_LUXURY: "Роскошь",
  REFRESH_BUSINESS_DEALS: "Обновить сделки",
  START_TURN: "Начать ход",
  HIRE_WORKER: "Нанять рабочего",
  PRODUCE_GOODS: "Произвести",
  SELL_GOODS: "Продать",
  ADJUST_POLICY: "Сдвиг политики",
  PLAY_CARD: "Карта / ручное",
  END_TURN: "Завершить ход",
};

export function phaseLabel(phase: RoundPhase | string): string {
  switch (phase) {
    case "PREPARATION":
      return "Подготовка";
    case "ACTIONS":
      return "Действия";
    case "PRODUCTION":
      return "Производство";
    case "VOTING":
      return "Голосование";
    case "SCORING":
      return "Подсчет";
    case "GAME_OVER":
      return "Игра окончена";
    default:
      return String(phase);
  }
}

export function resourceLabel(resourceId: string): string {
  const normalized = resourceId.toLowerCase();
  if (normalized === "food") return "еда";
  if (normalized === "luxury") return "роскошь";
  if (normalized === "healthcare") return "медицина";
  if (normalized === "education") return "образование";
  if (normalized === "influence" || normalized === "media_influence") return "влияние";
  return resourceId.replace(/[_-]+/g, " ");
}

export function compactActionSummary(summary: string): string {
  return summary
    .replace(/Advance one safe lifecycle step\.?/gi, "Безопасный переход по фазам.")
    .replace(/Assign up to (\d+) workers using assignment operations\.?/gi, "Назначение рабочих: до $1.")
    .replace(/Propose bill on ([A-Z0-9_]+) to ([ABC])\.?/gi, "Законопроект: $1 -> $2.")
    .replace(/Consume healthcare for population and increase welfare\.?/gi, "Потребить медицину.")
    .replace(/Consume education for population and increase welfare\.?/gi, "Потребить образование.")
    .replace(/Consume luxury for population and increase welfare\.?/gi, "Потребить роскошь.")
    .replace(/\bFOOD\b/g, "еда")
    .replace(/\bHEALTHCARE\b/g, "медицина")
    .replace(/\bEDUCATION\b/g, "образование")
    .replace(/\bLUXURY\b/g, "роскошь");
}

export function classTone(classType: ClassType | string | undefined): string {
  switch (classType) {
    case "WORKER":
      return "border-rose-500/45 bg-rose-500/10 text-rose-50";
    case "MIDDLE_CLASS":
      return "border-amber-400/45 bg-amber-400/10 text-amber-50";
    case "CAPITALIST":
      return "border-cyan-500/45 bg-cyan-500/10 text-cyan-50";
    case "STATE":
      return "border-stone-300/35 bg-stone-300/10 text-stone-50";
    default:
      return "border-zinc-700 bg-black/20 text-zinc-100";
  }
}
