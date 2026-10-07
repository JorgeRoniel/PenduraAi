import { ChangeDetectionStrategy, Component, ElementRef, effect, HostListener, input, output, ViewChild } from '@angular/core';

@Component({
  selector: 'app-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './app-modal.component.html',
  styleUrl: './app-modal.component.css'
})
export class AppModalComponent {
  readonly open = input(false);
  readonly title = input.required<string>();
  readonly labelledBy = input.required<string>();
  readonly closed = output<void>();
  @ViewChild('modalElement') private modalElement?: ElementRef<HTMLElement>;
  @ViewChild('closeButton') private closeButton?: ElementRef<HTMLButtonElement>;
  private previouslyFocused: HTMLElement | null = null;

  constructor() {
    effect(() => {
      if (this.open()) {
        this.previouslyFocused = document.activeElement as HTMLElement | null;
        setTimeout(() => this.closeButton?.nativeElement.focus());
      } else if (this.previouslyFocused?.isConnected) {
        this.previouslyFocused.focus();
        this.previouslyFocused = null;
      }
    });
  }

  @HostListener('document:keydown', ['$event'])
  handleKeydown(event: KeyboardEvent): void {
    if (!this.open()) return;
    if (event.key === 'Escape') {
      event.preventDefault();
      this.closed.emit();
      return;
    }
    if (event.key !== 'Tab') return;

    const focusable = Array.from(this.modalElement?.nativeElement.querySelectorAll<HTMLElement>(
      'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
    ) ?? []).filter((element) => element.getClientRects().length > 0);
    if (!focusable.length) return;

    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }
}
