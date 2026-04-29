import React from "react";

interface AppErrorBoundaryState {
  hasError: boolean;
  message?: string;
}

interface AppErrorBoundaryProps {
  children: React.ReactNode;
}

export class AppErrorBoundary extends React.Component<AppErrorBoundaryProps, AppErrorBoundaryState> {
  constructor(props: AppErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: unknown): AppErrorBoundaryState {
    return {
      hasError: true,
      message: error instanceof Error ? error.message : String(error),
    };
  }

  componentDidCatch(error: unknown) {
    // Keep a console trace so browser devtools show the original stack.
    // eslint-disable-next-line no-console
    console.error("UI render error", error);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex min-h-screen items-center justify-center bg-background px-6 text-foreground">
          <div className="w-full max-w-2xl rounded-xl border border-red-500/40 bg-red-500/10 p-5 text-sm">
            <p className="mb-2 font-semibold">Frontend render error</p>
            <p className="text-red-100">
              {this.state.message ?? "Unknown rendering error. Check browser console for details."}
            </p>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
