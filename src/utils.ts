export function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${(bytes / Math.pow(k, i)).toFixed(1)} ${sizes[i]}`;
}

export function formatDate(iso: string): string {
  try {
    const d = new Date(iso);
    const now = new Date();
    const sameDay = d.toDateString() === now.toDateString();
    if (sameDay) return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    return d.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
  } catch {
    return iso;
  }
}

export function getFileCategory(contentType: string, name: string): string {
  const t = contentType.toLowerCase();
  const ext = name.split('.').pop()?.toLowerCase() || '';
  if (t.startsWith('image/')) return 'image';
  if (t.startsWith('video/')) return 'video';
  if (t.startsWith('audio/')) return 'audio';
  if (t === 'application/pdf' || ext === 'pdf') return 'pdf';
  if (['doc', 'docx', 'odt'].includes(ext)) return 'doc';
  if (['xls', 'xlsx', 'csv', 'ods'].includes(ext)) return 'sheet';
  if (['ppt', 'pptx', 'odp'].includes(ext)) return 'slides';
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(ext)) return 'archive';
  if (['txt', 'md', 'json', 'xml', 'yml', 'yaml', 'log'].includes(ext)) return 'text';
  if (['js', 'ts', 'tsx', 'jsx', 'py', 'java', 'cpp', 'c', 'cs', 'go', 'rs', 'rb', 'php', 'html', 'css'].includes(ext)) return 'code';
  return 'file';
}
