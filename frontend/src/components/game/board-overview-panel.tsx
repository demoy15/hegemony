import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import type { GameState } from "@/types/game";

interface BoardOverviewPanelProps {
  state: GameState;
}

export function BoardOverviewPanel({ state }: BoardOverviewPanelProps) {
  const policyOrder = [
    "POLICY_1_FISCAL",
    "POLICY_2_LABOR_MARKET",
    "POLICY_3_TAXATION",
    "POLICY_4_HEALTHCARE_AND_BENEFITS",
    "POLICY_5_EDUCATION",
    "POLICY_6_FOREIGN_TRADE",
    "POLICY_7_IMMIGRATION",
  ] as const;
  const pendingPolicies = state.policies
    .filter((policy) => policy.occupyingProposalToken)
    .sort((a, b) => policyOrder.indexOf(a.id) - policyOrder.indexOf(b.id));

  const unemployedByClass = state.workers.reduce<Record<string, number>>((acc, worker) => {
    if (worker.location === "UNEMPLOYED") {
      acc[worker.classType] = (acc[worker.classType] ?? 0) + 1;
    }
    return acc;
  }, {});
  const preparationSummary = state.lastPreparationSummary;
  const scoringSummary = state.lastScoringSummary;
  const finalResult = state.finalResult;

  return (
    <Card className="h-full">
      <CardHeader>
        <CardTitle>Board Overview</CardTitle>
      </CardHeader>
      <CardContent className="space-y-5">
        <section className="rounded-xl border border-border bg-background/40 p-4">
          <h4 className="mb-2 text-sm font-semibold uppercase tracking-wide text-muted-foreground">Core Setup State</h4>
          <div className="grid gap-2 sm:grid-cols-2">
            <p className="text-sm">Treasury: <span className="font-semibold">{state.treasury}</span></p>
            <p className="text-sm">Tax multiplier: <span className="font-semibold">{state.taxMultiplier}</span></p>
            <p className="text-sm">Public healthcare: <span className="font-semibold">{state.publicServices.healthcare}</span></p>
            <p className="text-sm">Public education: <span className="font-semibold">{state.publicServices.education}</span></p>
            <p className="text-sm">Public media: <span className="font-semibold">{state.publicServices.mediaInfluence}</span></p>
            <p className="text-sm">Voting bag (W/M/C): <span className="font-semibold">{state.votingBag.worker}/{state.votingBag.middleClass}/{state.votingBag.capitalist}</span></p>
          </div>
        </section>

        <section className="rounded-xl border border-border bg-background/40 p-4">
          <h4 className="mb-2 text-sm font-semibold uppercase tracking-wide text-muted-foreground">Round Lifecycle</h4>
          <p className="text-sm">
            Current: <span className="font-semibold">round {state.currentRound ?? state.turnOrder.round}</span> / {state.maxRounds ?? 5}
          </p>
          <p className="text-sm">
            Phase: <span className="font-semibold">{state.currentPhase ?? state.turnOrder.phase}</span>
          </p>
          <p className="text-sm">
            Status: <span className="font-semibold">{state.gameStatus ?? "IN_PROGRESS"}</span>
          </p>
          {preparationSummary && (
            <div className="mt-3 rounded-lg border border-border/80 p-3 text-xs">
              <p className="font-semibold">Preparation summary (round {preparationSummary.round})</p>
              <p className="text-muted-foreground">Executed: {preparationSummary.executedSubsteps.join(", ") || "none"}</p>
              <p className="text-muted-foreground">Unsupported: {preparationSummary.unsupportedSubsteps.join(" | ") || "none"}</p>
            </div>
          )}
          {scoringSummary && (
            <div className="mt-3 rounded-lg border border-border/80 p-3 text-xs">
              <p className="font-semibold">Scoring summary (round {scoringSummary.round})</p>
              <p className="text-muted-foreground">Unsupported scoring: {scoringSummary.unsupportedSources.join(" | ") || "none"}</p>
              <div className="mt-2 space-y-1">
                {scoringSummary.players.map((row) => (
                  <p key={`score-${row.playerId}`} className="text-muted-foreground">
                    {row.playerId}: before={row.accumulatedBeforePhase}, gained={row.gainedThisPhase}, total={row.totalAfterPhase}
                  </p>
                ))}
              </div>
            </div>
          )}
          {state.lifecycleUnsupportedNotes?.length > 0 && (
            <p className="mt-2 text-xs text-muted-foreground">Unsupported lifecycle notes: {state.lifecycleUnsupportedNotes.join(" | ")}</p>
          )}
        </section>

        {finalResult && (
          <section className="rounded-xl border border-amber-500/40 bg-amber-500/10 p-4">
            <h4 className="mb-2 text-sm font-semibold uppercase tracking-wide text-amber-200">Final Result</h4>
            <p className="text-sm text-amber-100">Completed round: {finalResult.completedRound}</p>
            <p className="text-sm text-amber-100">
              Winner: {finalResult.tie ? `tie (${finalResult.winnerPlayerIds.join(", ")})` : finalResult.winnerPlayerIds[0] ?? "n/a"}
            </p>
            <div className="mt-2 space-y-1 text-xs text-amber-100/90">
              {finalResult.standings.map((standing) => (
                <p key={`standing-${standing.playerId}`}>
                  #{standing.rank} {standing.playerId} ({standing.classType}) - {standing.totalVp} VP
                </p>
              ))}
            </div>
            {finalResult.unsupportedNotes.length > 0 && (
              <p className="mt-2 text-xs text-amber-200">{finalResult.unsupportedNotes.join(" | ")}</p>
            )}
          </section>
        )}

        <section className="rounded-xl border border-border bg-background/40 p-4">
          <h4 className="mb-2 text-sm font-semibold uppercase tracking-wide text-muted-foreground">Policies</h4>
          <div className="space-y-2">
            {state.policies.map((policy) => (
              <div key={policy.id} className="rounded-lg border border-border/80 px-3 py-2">
                <div className="flex items-center justify-between text-sm">
                  <span className="font-medium">{policy.id}</span>
                  <Badge tone="warning">Course {policy.currentCourse}</Badge>
                </div>
                {policy.occupyingProposalToken ? (
                  <p className="mt-1 text-xs text-muted-foreground">
                    Pending proposal: {policy.occupyingProposalToken.ownerPlayerId ?? policy.occupyingProposalToken.ownerClass} &rarr;{" "}
                    {policy.occupyingProposalToken.targetCourse}
                  </p>
                ) : (
                  <p className="mt-1 text-xs text-muted-foreground">No pending proposal token</p>
                )}
              </div>
            ))}
          </div>
        </section>

        <section className="rounded-xl border border-border bg-background/40 p-4">
          <h4 className="mb-2 text-sm font-semibold uppercase tracking-wide text-muted-foreground">Voting Panel</h4>
          <p className="text-sm">
            Current phase: <span className="font-semibold">{state.turnOrder.phase}</span>
          </p>
          <div className="mt-2 space-y-1">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">Pending proposals (policy order)</p>
            {pendingPolicies.length === 0 && <p className="text-xs text-muted-foreground">No pending proposals.</p>}
            {pendingPolicies.map((policy) => (
              <p key={`pending-${policy.id}`} className="text-xs text-muted-foreground">
                {policy.id}: {policy.currentCourse} &rarr; {policy.occupyingProposalToken?.targetCourse} by{" "}
                {policy.occupyingProposalToken?.ownerPlayerId ?? policy.occupyingProposalToken?.ownerClass}
              </p>
            ))}
          </div>

          {state.currentVoteState ? (
            <div className="mt-3 space-y-2 rounded-lg border border-border/80 p-3 text-xs">
              <p>
                Active vote: <span className="font-semibold">{state.currentVoteState.activeProposalPolicyId}</span>
              </p>
              <p>
                Proposer: {state.currentVoteState.proposalAuthorPlayerId} | Course {state.currentVoteState.currentCourseBeforeVote} &rarr;{" "}
                {state.currentVoteState.targetCourse}
              </p>
              <p>Stage: {state.currentVoteState.votingStage}</p>
              <p>
                Stances submitted: {Object.keys(state.currentVoteState.stanceByPlayer).length}/{state.players.length}
              </p>
              <div className="space-y-1">
                {Object.entries(state.currentVoteState.stanceByPlayer).length === 0 && <p className="text-muted-foreground">No stances yet.</p>}
                {Object.entries(state.currentVoteState.stanceByPlayer).map(([playerId, playerStance]) => (
                  <p key={`stance-${playerId}`} className="text-muted-foreground">
                    {playerId}: {playerStance}
                  </p>
                ))}
              </div>
              <p>
                Influence submitted: {Object.keys(state.currentVoteState.influenceCommitments).length}/{state.players.length}
              </p>
              <div className="space-y-1">
                {Object.entries(state.currentVoteState.influenceCommitments).length === 0 && (
                  <p className="text-muted-foreground">No influence commits yet.</p>
                )}
                {Object.entries(state.currentVoteState.influenceCommitments).map(([playerId, amount]) => (
                  <p key={`commit-${playerId}`} className="text-muted-foreground">
                    {playerId}: {amount}
                  </p>
                ))}
              </div>
              <p>
                Totals: FOR {state.currentVoteState.totalForVotes} / AGAINST {state.currentVoteState.totalAgainstVotes}
              </p>
              <div className="space-y-1">
                <p className="text-muted-foreground">Drawn cubes:</p>
                {state.currentVoteState.drawnVotingCubes.length === 0 && <p className="text-muted-foreground">none yet</p>}
                {state.currentVoteState.drawnVotingCubes.map((cube, idx) => (
                  <p key={`cube-${idx}`} className="text-muted-foreground">
                    {cube.ownerClass} &rarr; {cube.interpretedVote}
                  </p>
                ))}
              </div>
            </div>
          ) : (
            <p className="mt-2 text-xs text-muted-foreground">No active voting session.</p>
          )}

          {state.lastProposalResolution && (
            <div className="mt-2 rounded-lg border border-border/80 p-2 text-xs text-muted-foreground">
              Last result: {state.lastProposalResolution.policyId} {state.lastProposalResolution.fromCourse} &rarr;{" "}
              {state.lastProposalResolution.targetCourse} ({state.lastProposalResolution.result})
            </div>
          )}
        </section>

        <section className="rounded-xl border border-border bg-background/40 p-4">
          <h4 className="mb-2 text-sm font-semibold uppercase tracking-wide text-muted-foreground">Economic Panel</h4>
          <div className="space-y-2 text-xs text-muted-foreground">
            {state.players.map((player) => (
              <div key={`money-${player.playerId}`} className="rounded-lg border border-border/80 p-2">
                <p>
                  {player.playerId} ({player.classType}): money={player.money}, revenue={player.revenue}, capital={player.capital}, population={player.population},
                  welfare={player.welfare} (delta={player.lastWelfareDelta ?? 0})
                </p>
                <p>goods_and_services_area: {JSON.stringify(player.goodsAndServicesArea ?? {})}</p>
                <p>produced_resource_storage: {JSON.stringify(player.producedResourceStorage ?? {})}</p>
                <p>prices: {JSON.stringify(player.prices ?? {})}</p>
              </div>
            ))}
            <p>State treasury: {state.treasury}</p>
            <p>State public_services_storage: {JSON.stringify(state.publicServicesStorage ?? {})}</p>
            {state.economyUnsupportedNotes?.length > 0 && (
              <p>Economy unsupported notes: {state.economyUnsupportedNotes.join(" | ")}</p>
            )}
          </div>

          {state.productionPhaseState && (
            <div className="mt-3 space-y-2 rounded-lg border border-border/80 p-3 text-xs">
              <p>
                Production stage: <span className="font-semibold">{state.productionPhaseState.stage}</span>
              </p>
              <p>
                Food worker: {state.productionPhaseState.workerFoodConsumed}/{state.productionPhaseState.workerFoodRequired}
                {state.productionPhaseState.workerFoodUnmet > 0 ? ` (unmet ${state.productionPhaseState.workerFoodUnmet})` : ""}
              </p>
              <p>
                Food middle class: {state.productionPhaseState.middleClassFoodConsumed}/{state.productionPhaseState.middleClassFoodRequired}
                {state.productionPhaseState.middleClassFoodUnmet > 0 ? ` (unmet ${state.productionPhaseState.middleClassFoodUnmet})` : ""}
              </p>
              <p>
                Taxes paid: worker={state.productionPhaseState.workerTaxesPaid}, middle={state.productionPhaseState.middleClassTaxesPaid}, capitalist={state.productionPhaseState.capitalistTaxesPaid}
              </p>
              <p>
                Flags: insufficientFood={String(state.productionPhaseState.insufficientFood)}, unsupportedMiddleTax={String(
                  state.productionPhaseState.unsupportedMiddleClassTaxModel,
                )}, unsupportedCapitalistTax={String(state.productionPhaseState.unsupportedCapitalistTaxModel)}
              </p>
              <div className="space-y-1">
                <p className="text-muted-foreground">Enterprise production results:</p>
                {state.productionPhaseState.enterpriseResults.length === 0 && <p className="text-muted-foreground">none</p>}
                {state.productionPhaseState.enterpriseResults.map((result) => (
                  <p key={`prod-${result.enterpriseId}`} className="text-muted-foreground">
                    {result.enterpriseId}: functioning={String(result.functioning)}, wages={JSON.stringify(result.wagesPaidByRecipient)}, produced={JSON.stringify(
                      result.producedResources,
                    )}
                  </p>
                ))}
              </div>
            </div>
          )}

          {!state.productionPhaseState && state.lastProductionSummary && (
            <div className="mt-2 rounded-lg border border-border/80 p-2 text-xs text-muted-foreground">
              Last production summary: stage={state.lastProductionSummary.stage}, workerTax={state.lastProductionSummary.workerTaxesPaid},
              workerFoodUnmet={state.lastProductionSummary.workerFoodUnmet}, middleFoodUnmet={state.lastProductionSummary.middleClassFoodUnmet}
            </div>
          )}
        </section>

        <section className="rounded-xl border border-border bg-background/40 p-4">
          <h4 className="mb-2 text-sm font-semibold uppercase tracking-wide text-muted-foreground">Workers</h4>
          <p className="mb-2 text-xs text-muted-foreground">
            Unemployed: {Object.entries(unemployedByClass).map(([k, v]) => `${k}: ${v}`).join(" | ") || "none"}
          </p>
          <div className="max-h-40 space-y-2 overflow-y-auto pr-1">
            {state.workers.map((worker) => (
              <div key={worker.id} className="rounded-lg border border-border/80 px-3 py-2 text-xs">
                <p>
                  {worker.id} ({worker.classType}, {worker.qualificationType}{worker.sector ? `/${worker.sector}` : ""})
                </p>
                <p className="text-muted-foreground">
                  {worker.location}
                  {worker.enterpriseId ? ` @ ${worker.enterpriseId}/${worker.slotId}` : ""}
                  {worker.tiedContract ? " | tied contract" : " | free"}
                </p>
              </div>
            ))}
          </div>
        </section>

        <section className="rounded-xl border border-border bg-background/40 p-4">
          <h4 className="mb-2 text-sm font-semibold uppercase tracking-wide text-muted-foreground">Enterprises</h4>
          <div className="max-h-56 space-y-2 overflow-y-auto pr-1">
            {state.enterprises.map((enterprise) => (
              <div key={enterprise.id} className="rounded-lg border border-border/80 p-3 text-xs">
                <div className="mb-1 flex items-center justify-between">
                  <p className="font-medium">{enterprise.id}</p>
                  <Badge tone={enterprise.functioning ? "positive" : "warning"}>
                    {enterprise.functioning ? "functioning" : "not functioning"}
                  </Badge>
                </div>
                <p className="text-muted-foreground">Owner: {enterprise.ownerClass} | Sector: {enterprise.sector} | Wage: {enterprise.wageLevel}</p>
                <p className="text-muted-foreground">Produces: {JSON.stringify(enterprise.producedResources)}</p>
                <div className="mt-2 space-y-1">
                  {enterprise.slots.map((slot) => (
                    <p key={slot.id} className="text-muted-foreground">
                      {slot.id}: {slot.requiredQualification}
                      {slot.requiredSector ? `/${slot.requiredSector}` : ""} &rarr; {slot.occupiedWorkerId ?? "empty"}
                    </p>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </section>
      </CardContent>
    </Card>
  );
}
