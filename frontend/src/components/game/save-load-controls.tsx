import { Save, Upload, RotateCcw } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";

interface SaveLoadControlsProps {
  fileName: string;
  setFileName: (name: string) => void;
  onSave: () => void;
  onLoad: () => void;
  onReset: () => void;
  isSaving: boolean;
  isLoading: boolean;
  isResetting: boolean;
}

export function SaveLoadControls({
  fileName,
  setFileName,
  onSave,
  onLoad,
  onReset,
  isSaving,
  isLoading,
  isResetting,
}: SaveLoadControlsProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Save / Load</CardTitle>
        <CardDescription>JSON persistence for MVP iteration.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <Input value={fileName} onChange={(e) => setFileName(e.target.value)} placeholder="demo-save.json" />
        <div className="grid grid-cols-3 gap-2">
          <Button variant="secondary" onClick={onSave} disabled={isSaving}>
            <Save size={14} className="mr-2" />
            Save
          </Button>
          <Button variant="secondary" onClick={onLoad} disabled={isLoading}>
            <Upload size={14} className="mr-2" />
            Load
          </Button>
          <Button variant="outline" onClick={onReset} disabled={isResetting}>
            <RotateCcw size={14} className="mr-2" />
            Reset
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
