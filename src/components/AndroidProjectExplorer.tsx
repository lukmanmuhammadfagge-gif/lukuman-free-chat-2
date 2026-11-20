import React, { useMemo, useState } from 'react';
import JSZip from 'jszip';
import { Check, Copy, Download, FileCode2, FolderTree } from 'lucide-react';
import { WebStrings } from '../i18n';

const androidRawFiles = import.meta.glob(
  [
    '/app/**/*',
    '/gradle/wrapper/*.properties',
    '/build.gradle.kts',
    '/settings.gradle.kts',
    '/gradle.properties',
    '/gradlew',
    '/gradlew.bat',
    '/firestore.rules',
    '/README.md',
    '/.github/workflows/build-apk.yml',
  ],
  { query: '?raw', import: 'default', eager: true }
) as Record<string, string>;

interface AndroidProjectExplorerProps {
  strings: WebStrings;
}

export const AndroidProjectExplorer: React.FC<AndroidProjectExplorerProps> = ({ strings }) => {
  const filePaths = useMemo(() => {
    return Object.keys(androidRawFiles)
      .map((p) => (p.startsWith('/') ? p.slice(1) : p))
      .sort((a, b) => {
        const priority = (path: string) => {
          if (path === 'README.md') return 0;
          if (path === '.github/workflows/build-apk.yml') return 1;
          if (path === 'firestore.rules') return 2;
          if (path === 'app/build.gradle.kts') return 3;
          if (path.endsWith('.kt')) return 4;
          return 5;
        };
        const diff = priority(a) - priority(b);
        return diff !== 0 ? diff : a.localeCompare(b);
      });
  }, []);

  const [selectedPath, setSelectedPath] = useState<string>(
    () => filePaths[0] || 'README.md'
  );
  const [copied, setCopied] = useState(false);
  const [downloading, setDownloading] = useState(false);

  const selectedContent = androidRawFiles[`/${selectedPath}`] ?? '';

  const handleCopy = async () => {
    await navigator.clipboard.writeText(selectedContent);
    setCopied(true);
    setTimeout(() => setCopied(false), 1800);
  };

  const handleDownloadZip = async () => {
    setDownloading(true);
    try {
      const zip = new JSZip();
      for (const relPath of filePaths) {
        const content = androidRawFiles[`/${relPath}`];
        if (typeof content === 'string') {
          if (relPath === 'gradlew') {
            zip.file(relPath, content, { unixPermissions: '755' });
          } else {
            zip.file(relPath, content);
          }
        }
      }

      try {
        const res = await fetch('/gradle-wrapper.jar');
        if (res.ok) {
          const buf = await res.arrayBuffer();
          zip.file('gradle/wrapper/gradle-wrapper.jar', buf);
        }
      } catch {
        // Optional binary fallback
      }

      try {
        const ksRes = await fetch('/lukuman-debug.keystore');
        if (ksRes.ok) {
          const ksBuf = await ksRes.arrayBuffer();
          zip.file('lukuman-debug.keystore', ksBuf);
        }
      } catch {
        // Optional binary fallback
      }

      const blob = await zip.generateAsync({ type: 'blob' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'lukuman-free-chat-android.zip';
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto w-full px-4 sm:px-6 py-6 space-y-6">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-[#213C73] pb-5">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-[#F0F6FF]">
            {strings.androidProjectTitle}
          </h1>
          <p className="text-sm text-[#96ACD0] mt-1">
            Package com.lukuman.freechat · Min SDK 26 · Target SDK 35 · ./gradlew :app:assembleDebug
          </p>
        </div>

        <button
          type="button"
          onClick={handleDownloadZip}
          disabled={downloading}
          className="inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-[#1D4ED8] hover:bg-[#2563EB] disabled:opacity-60 text-sm font-semibold text-[#F0F6FF] transition-colors whitespace-nowrap shrink-0 cursor-pointer"
        >
          <Download className="w-4 h-4" />
          <span>{downloading ? strings.downloadingZip : strings.downloadZip}</span>
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 min-h-[560px]">
        <aside className="lg:col-span-4 bg-[#0C1B3B] border border-[#213C73] rounded-2xl overflow-hidden flex flex-col">
          <div className="px-4 py-3 border-b border-[#213C73] flex items-center gap-2 text-xs font-semibold text-[#38BDF8]">
            <FolderTree className="w-4 h-4" />
            <span>Project Files ({filePaths.length})</span>
          </div>
          <div className="overflow-y-auto max-h-[540px] divide-y divide-[#173166]/60">
            {filePaths.map((path) => {
              const active = path === selectedPath;
              return (
                <button
                  key={path}
                  type="button"
                  onClick={() => setSelectedPath(path)}
                  className={`w-full text-left px-4 py-2.5 text-xs font-mono-code flex items-center gap-2.5 transition-colors cursor-pointer ${
                    active
                      ? 'bg-[#173166] text-[#F0F6FF] font-semibold'
                      : 'text-[#96ACD0] hover:bg-[#122650] hover:text-[#F0F6FF]'
                  }`}
                >
                  <FileCode2 className="w-3.5 h-3.5 shrink-0 text-[#38BDF8]" />
                  <span className="truncate">{path}</span>
                </button>
              );
            })}
          </div>
        </aside>

        <section className="lg:col-span-8 bg-[#0C1B3B] border border-[#213C73] rounded-2xl overflow-hidden flex flex-col">
          <div className="px-4 py-3 border-b border-[#213C73] flex items-center justify-between gap-4">
            <span className="text-xs font-mono-code text-[#F0F6FF] truncate">
              {selectedPath}
            </span>
            <button
              type="button"
              onClick={handleCopy}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#122650] hover:bg-[#173166] border border-[#213C73] text-xs font-medium text-[#F0F6FF] transition-colors whitespace-nowrap shrink-0 cursor-pointer"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-[#38BDF8]" /> : <Copy className="w-3.5 h-3.5" />}
              <span>{copied ? 'Copied' : 'Copy'}</span>
            </button>
          </div>
          <pre className="p-4 text-xs leading-relaxed font-mono-code text-[#E2ECFF] overflow-auto max-h-[540px] whitespace-pre">
            {selectedContent}
          </pre>
        </section>
      </div>
    </div>
  );
};
