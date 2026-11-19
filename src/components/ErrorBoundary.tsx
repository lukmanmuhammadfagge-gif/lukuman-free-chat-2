import React from 'react';

interface ErrorBoundaryProps {
  children: React.ReactNode;
}

interface ErrorBoundaryState {
  errorMessage: string | null;
}

export class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { errorMessage: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { errorMessage: error.message || 'An unexpected error occurred.' };
  }

  componentDidCatch(error: Error) {
    console.error('ErrorBoundary caught:', error);
  }

  render() {
    if (this.state.errorMessage) {
      return (
        <div className="min-h-screen bg-[#071126] text-[#F0F6FF] flex items-center justify-center p-6">
          <div className="max-w-lg w-full bg-[#0C1B3B] border border-[#213C73] rounded-2xl p-6 space-y-4">
            <h2 className="text-lg font-semibold text-[#F0F6FF]">Application Error</h2>
            <p className="text-sm text-[#96ACD0] break-words font-mono-code">
              {this.state.errorMessage}
            </p>
            <button
              type="button"
              onClick={() => this.setState({ errorMessage: null })}
              className="px-4 py-2 rounded-xl bg-[#1D4ED8] hover:bg-[#2563EB] text-sm font-medium text-[#F0F6FF] transition-colors"
            >
              Reload View
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
