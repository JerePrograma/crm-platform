export type DecisionChoice = {
  value: string;
  label: string;
  description?: string;
};

export type DecisionDialogOptions = {
  title: string;
  description: string;
  confirmLabel: string;
  cancelLabel?: string;
  danger?: boolean;
  input?: {
    label: string;
    initialValue?: string;
    placeholder?: string;
    required?: boolean;
  };
  choices?: DecisionChoice[];
};

export function openDecisionDialog(options: DecisionDialogOptions): Promise<string | null> {
  return new Promise((resolve) => {
    const previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const overlay = document.createElement("div");
    overlay.className = "dialog-overlay";

    const dialog = document.createElement("section");
    dialog.className = "decision-dialog";
    dialog.setAttribute("role", "dialog");
    dialog.setAttribute("aria-modal", "true");
    dialog.tabIndex = -1;

    const titleId = `decision-title-${crypto.randomUUID()}`;
    const descriptionId = `decision-description-${crypto.randomUUID()}`;
    dialog.setAttribute("aria-labelledby", titleId);
    dialog.setAttribute("aria-describedby", descriptionId);

    const heading = document.createElement("h2");
    heading.id = titleId;
    heading.textContent = options.title;
    const description = document.createElement("p");
    description.id = descriptionId;
    description.textContent = options.description;

    const form = document.createElement("form");
    form.className = "decision-dialog-form";
    form.append(heading, description);

    let valueControl: HTMLInputElement | HTMLSelectElement | null = null;
    if (options.input) {
      const label = document.createElement("label");
      label.textContent = options.input.label;
      const input = document.createElement("input");
      input.value = options.input.initialValue ?? "";
      input.placeholder = options.input.placeholder ?? "";
      input.required = options.input.required ?? true;
      label.append(input);
      form.append(label);
      valueControl = input;
    } else if (options.choices) {
      const label = document.createElement("label");
      label.textContent = "Seleccioná el registro";
      const select = document.createElement("select");
      select.required = true;
      const placeholder = document.createElement("option");
      placeholder.value = "";
      placeholder.textContent = "Seleccionar…";
      select.append(placeholder);
      for (const choice of options.choices) {
        const option = document.createElement("option");
        option.value = choice.value;
        option.textContent = choice.description ? `${choice.label} — ${choice.description}` : choice.label;
        select.append(option);
      }
      label.append(select);
      form.append(label);
      valueControl = select;
    }

    const actions = document.createElement("div");
    actions.className = "decision-dialog-actions";
    const cancel = document.createElement("button");
    cancel.type = "button";
    cancel.className = "secondary-button";
    cancel.textContent = options.cancelLabel ?? "Cancelar";
    const confirm = document.createElement("button");
    confirm.type = "submit";
    confirm.className = options.danger ? "danger-button" : "primary-button";
    confirm.textContent = options.confirmLabel;
    actions.append(cancel, confirm);
    form.append(actions);
    dialog.append(form);
    overlay.append(dialog);
    document.body.append(overlay);
    document.body.classList.add("dialog-open");

    let resolved = false;
    const finish = (value: string | null) => {
      if (resolved) return;
      resolved = true;
      document.removeEventListener("keydown", handleKeys, true);
      overlay.remove();
      document.body.classList.remove("dialog-open");
      previousFocus?.focus();
      resolve(value);
    };

    const focusable = () =>
      Array.from(
        dialog.querySelectorAll<HTMLElement>(
          'button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
        ),
      );

    const handleKeys = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        finish(null);
        return;
      }
      if (event.key !== "Tab") return;
      const controls = focusable();
      if (!controls.length) return;
      const first = controls[0]!;
      const last = controls.at(-1)!;
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };

    document.addEventListener("keydown", handleKeys, true);
    cancel.addEventListener("click", () => finish(null));
    overlay.addEventListener("mousedown", (event) => {
      if (event.target === overlay) finish(null);
    });
    form.addEventListener("submit", (event) => {
      event.preventDefault();
      if (!form.reportValidity()) return;
      finish(valueControl?.value.trim() || "confirmed");
    });

    window.requestAnimationFrame(() => (valueControl ?? cancel).focus());
  });
}
