import { useEffect, useMemo, useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { useUiStore } from "@/store/use-ui-store";
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
  const selectedTemplate = useMemo(
    () => composerMetadata.actionTemplates.find((template) => template.actionType === selectedAction),
    [composerMetadata.actionTemplates, selectedAction],
  );
  const isChosenActionLegal = legalMoveTypes.has(selectedAction);

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
    return { actorPlayerId };
  };

  const submitPreview = () => onPreview(selectedAction, buildParameters(), optionalModifier, optionalCardReference);
  const submitCommand = () => onSubmit(selectedAction, buildParameters());

  return (
    <Card>
      <CardHeader>
        <CardTitle>Human Action Composer</CardTitle>
        <CardDescription>Guided flow: choose action, preview delta, then confirm.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
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
