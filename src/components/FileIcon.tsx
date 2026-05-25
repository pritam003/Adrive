import { Folder, File, Image, FileText, FileVideo, FileAudio, FileArchive, FileCode } from 'lucide-react';
import { getFileCategory } from '../utils';

interface Props {
  category?: string;
  contentType?: string;
  name?: string;
  size?: number;
  className?: string;
  folder?: boolean;
}

export function FileIcon({ category, contentType = '', name = '', size = 24, className, folder }: Props) {
  if (folder) return <Folder size={size} className={className} color="#5f6368" />;
  const cat = category || getFileCategory(contentType, name);
  const color = (() => {
    switch (cat) {
      case 'image': return '#34a853';
      case 'video': return '#ea4335';
      case 'audio': return '#fbbc04';
      case 'pdf': return '#ea4335';
      case 'doc': return '#4285f4';
      case 'sheet': return '#34a853';
      case 'slides': return '#fbbc04';
      case 'archive': return '#9aa0a6';
      case 'code': return '#9c27b0';
      default: return '#5f6368';
    }
  })();
  switch (cat) {
    case 'image': return <Image size={size} className={className} color={color} />;
    case 'video': return <FileVideo size={size} className={className} color={color} />;
    case 'audio': return <FileAudio size={size} className={className} color={color} />;
    case 'archive': return <FileArchive size={size} className={className} color={color} />;
    case 'code': return <FileCode size={size} className={className} color={color} />;
    case 'text':
    case 'pdf':
    case 'doc':
    case 'sheet':
    case 'slides':
      return <FileText size={size} className={className} color={color} />;
    default:
      return <File size={size} className={className} color={color} />;
  }
}
