export function normalizePhone(input: string) {
  const digits = input.replace(/\D/g, "");
  if (!digits) return "";
  let cleaned = digits;
  if (cleaned.length === 11 && cleaned.startsWith("8")) {
    cleaned = "7" + cleaned.slice(1);
  }
  if (!cleaned.startsWith("7") && cleaned.length === 10) {
    cleaned = "7" + cleaned;
  }
  return "+" + cleaned;
}

export function formatPhone(input: string) {
  const normalized = normalizePhone(input);
  const digits = normalized.replace(/\D/g, "");
  if (!digits) return "";
  const part = digits.slice(1); // after country
  const a = part.slice(0, 3);
  const b = part.slice(3, 6);
  const c = part.slice(6, 8);
  const d = part.slice(8, 10);
  let formatted = "+7";
  if (a) formatted += ` (${a}`;
  if (a && a.length === 3) formatted += ")";
  if (b) formatted += ` ${b}`;
  if (c) formatted += `-${c}`;
  if (d) formatted += `-${d}`;
  return formatted;
}
