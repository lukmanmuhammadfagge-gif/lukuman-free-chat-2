const STRIP_REGEX = /[\s\-().]+/g;
const NIGERIAN_E164_REGEX = /^\+234[1-9]\d{9}$/;

/**
 * Normalizes Nigerian phone numbers into canonical E.164 format (+234XXXXXXXXXX).
 * Accepted inputs include +2348012345678, +234 801 234 5678, 2348012345678,
 * 08012345678, and 10-digit mobile numbers starting with 7/8/9 (8012345678).
 */
export function normalizeNigerianPhone(raw: string): string | null {
  const cleaned = raw.trim().replace(STRIP_REGEX, '');
  if (!cleaned) return null;

  let candidate: string;
  if (cleaned.startsWith('+2340') && cleaned.length === 15) {
    candidate = `+234${cleaned.slice(5)}`;
  } else if (cleaned.startsWith('+234')) {
    candidate = cleaned;
  } else if (cleaned.startsWith('2340') && cleaned.length === 14) {
    candidate = `+234${cleaned.slice(4)}`;
  } else if (cleaned.startsWith('234') && cleaned.length === 13) {
    candidate = `+${cleaned}`;
  } else if (cleaned.startsWith('0') && cleaned.length === 11) {
    candidate = `+234${cleaned.slice(1)}`;
  } else if (cleaned.length === 10 && ['7', '8', '9'].includes(cleaned[0])) {
    candidate = `+234${cleaned}`;
  } else {
    return null;
  }

  return NIGERIAN_E164_REGEX.test(candidate) ? candidate : null;
}
