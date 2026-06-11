export interface ClassBoardSectionLayout {
  id: string;
  label: string;
}

export interface ClassBoardLayout {
  boardId: "WORKER" | "MIDDLE_CLASS" | "CAPITALIST" | "STATE";
  accentClass: string;
  sections: ClassBoardSectionLayout[];
}

export const CLASS_BOARD_LAYOUTS: ClassBoardLayout[] = [
  {
    boardId: "WORKER",
    accentClass: "border-rose-500/55 bg-gradient-to-br from-rose-950/70 via-zinc-900/90 to-zinc-950",
    sections: [
      { id: "population", label: "Население" },
      { id: "welfare", label: "Благосостояние" },
      { id: "income", label: "Деньги" },
      { id: "goods", label: "Товары и услуги" },
      { id: "unions", label: "Профсоюзы" },
    ],
  },
  {
    boardId: "MIDDLE_CLASS",
    accentClass: "border-amber-400/55 bg-gradient-to-br from-amber-950/65 via-zinc-900/90 to-zinc-950",
    sections: [
      { id: "population", label: "Население" },
      { id: "welfare", label: "Благосостояние" },
      { id: "income", label: "Деньги" },
      { id: "goods", label: "Товары и услуги" },
      { id: "prices", label: "Цены" },
    ],
  },
  {
    boardId: "CAPITALIST",
    accentClass: "border-sky-500/55 bg-gradient-to-br from-sky-950/70 via-zinc-900/90 to-zinc-950",
    sections: [
      { id: "revenue", label: "Доход" },
      { id: "capital", label: "Капитал" },
      { id: "wealth", label: "Победные очки" },
      { id: "storage", label: "Склад ресурсов" },
      { id: "prices", label: "Цены" },
    ],
  },
  {
    boardId: "STATE",
    accentClass: "border-slate-400/55 bg-gradient-to-br from-slate-900/75 via-zinc-900/90 to-zinc-950",
    sections: [
      { id: "treasury", label: "Казна" },
      { id: "legitimacy", label: "Легитимность" },
      { id: "influence", label: "Влияние" },
      { id: "services", label: "Госуслуги" },
      { id: "goods", label: "Запасы" },
    ],
  },
];
