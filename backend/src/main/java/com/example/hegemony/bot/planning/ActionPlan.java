package com.example.hegemony.bot.planning;

import com.example.hegemony.domain.command.GameCommand;

public class ActionPlan {
    private GameCommand mainAction;
    private String optionalModifier;
    private String optionalCardReference;
    private ActionPreviewDelta previewDelta;
    private ActionPlanSupportStatus supportStatus = ActionPlanSupportStatus.SUPPORTED;
    private String supportNote;

    public ActionPlan() {
    }

    public ActionPlan(GameCommand mainAction, ActionPlanSupportStatus supportStatus, String supportNote) {
        this.mainAction = mainAction;
        this.supportStatus = supportStatus == null ? ActionPlanSupportStatus.SUPPORTED : supportStatus;
        this.supportNote = supportNote;
    }

    public GameCommand getMainAction() {
        return mainAction;
    }

    public void setMainAction(GameCommand mainAction) {
        this.mainAction = mainAction;
    }

    public String getOptionalModifier() {
        return optionalModifier;
    }

    public void setOptionalModifier(String optionalModifier) {
        this.optionalModifier = optionalModifier;
    }

    public String getOptionalCardReference() {
        return optionalCardReference;
    }

    public void setOptionalCardReference(String optionalCardReference) {
        this.optionalCardReference = optionalCardReference;
    }

    public ActionPreviewDelta getPreviewDelta() {
        return previewDelta;
    }

    public void setPreviewDelta(ActionPreviewDelta previewDelta) {
        this.previewDelta = previewDelta;
    }

    public ActionPlanSupportStatus getSupportStatus() {
        return supportStatus;
    }

    public void setSupportStatus(ActionPlanSupportStatus supportStatus) {
        this.supportStatus = supportStatus == null ? ActionPlanSupportStatus.SUPPORTED : supportStatus;
    }

    public String getSupportNote() {
        return supportNote;
    }

    public void setSupportNote(String supportNote) {
        this.supportNote = supportNote;
    }
}
