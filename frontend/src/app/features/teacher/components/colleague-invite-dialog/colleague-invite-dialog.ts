import { ChangeDetectionStrategy, Component, effect, inject, model, output } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';

import { ApiError } from '../../../../core/api/problem-details';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { createSubmitGuard } from '../../../../core/util/submitting';
import { FormFieldComponent } from '../../../../shared/components/form-field/form-field';
import { SubmitButtonComponent } from '../../../../shared/components/submit-button/submit-button';
import {
  applyFieldErrors,
  autoClearServerErrors,
  markAllTouched,
} from '../../../../shared/forms/apply-field-errors';
import { TeacherColleagueService } from '../../data/teacher-colleague.service';
import type { TeacherColleague } from '../../models/colleague';

/**
 * Convite de um colega professor.
 *
 * Não há campo de senha nem de instituição: o colega entra na mesma instituição
 * de quem convida e define a própria senha pelo link do e-mail — o mesmo fluxo
 * do convite feito pelo admin.
 */
@Component({
  selector: 'cc-colleague-invite-dialog',
  imports: [
    ReactiveFormsModule,
    Dialog,
    Button,
    InputText,
    Message,
    FormFieldComponent,
    SubmitButtonComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './colleague-invite-dialog.html',
  styleUrl: './colleague-invite-dialog.scss',
})
export class ColleagueInviteDialogComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly colleagues = inject(TeacherColleagueService);
  private readonly notify = inject(NotificationService);

  readonly visible = model(false);
  readonly saved = output<TeacherColleague>();

  protected readonly guard = createSubmitGuard();

  protected readonly form = this.fb.group({
    fullName: ['', [Validators.required, Validators.maxLength(160)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    registrationNumber: ['', [Validators.required, Validators.maxLength(80)]],
  });

  constructor() {
    autoClearServerErrors(this.form);

    effect(() => {
      if (this.visible()) {
        this.form.reset();
      }
    });
  }

  protected submit(): void {
    markAllTouched(this.form);
    if (this.form.invalid) {
      return;
    }

    void this.guard.run(async () => {
      const value = this.form.getRawValue();
      try {
        const invited = await this.colleagues.invite({
          fullName: value.fullName.trim(),
          email: value.email.trim().toLowerCase(),
          registrationNumber: value.registrationNumber.trim(),
        });
        this.notify.success(
          'Convite enviado',
          `${invited.fullName} vai receber um e-mail para definir a própria senha.`,
        );
        this.saved.emit(invited);
        this.visible.set(false);
      } catch (error) {
        this.handleError(error);
      }
    });
  }

  private handleError(error: unknown): void {
    if (!(error instanceof ApiError)) {
      this.notify.error('Algo deu errado. Tente novamente.');
      return;
    }
    if (error.code === 'EMAIL_ALREADY_REGISTERED') {
      applyFieldErrors(this.form, [
        { field: 'email', message: 'Já existe uma conta com este e-mail.' },
      ]);
      return;
    }
    if (error.fieldErrors.length > 0) {
      const { orphans } = applyFieldErrors(this.form, error);
      if (orphans.length > 0) {
        this.notify.error(orphans.map((o) => o.message).join(' '), 'Verifique os dados');
      }
      return;
    }
    this.notify.error(error);
  }
}
