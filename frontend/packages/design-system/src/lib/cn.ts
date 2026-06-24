import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/** Merge condizionale di classi Tailwind (clsx + tailwind-merge per risolvere i conflitti). */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs))
}
