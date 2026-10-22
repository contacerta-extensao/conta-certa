import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Paginator, type PaginatorState } from 'primeng/paginator';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';

import type { AccountStatus } from '../../../../core/models/enums';
import { ACCOUNT_STATUS_LABELS } from '../../../../core/models/labels';
import type { PageQuery } from '../../../../core/models/page';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state';
import { LoadingSkeletonComponent } from '../../../../shared/components/loading-skeleton/loading-skeleton';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header';
import { StatusTagComponent } from '../../../../shared/components/status-tag/status-tag';
import { createPageState } from '../../../../shared/forms/page-state';
import { ColleagueInviteDialogComponent } from '../../components/colleague-invite-dialog/colleague-invite-dialog';
import { TeacherColleagueService } from '../../data/teacher-colleague.service';

const STATUSES: readonly AccountStatus[] = ['PENDING', 'ACTIVE', 'INACTIVE'];
const SEARCH_DEBOUNCE_MS = 300;

/**
 * Professores da instituição.
 *
 * O professor vê os colegas da própria instituição e pode convidar novos. O
 * convite cria a conta `PENDING`; ativar, desativar e editar continuam sendo
 * tarefas do admin.
 */
@Component({
  selector: 'cc-teacher-colleagues-page',
  imports: [
    FormsModule,
    Button,
    InputText,
    Select,
    Paginator,
    TableModule,
    PageHeaderComponent,
    LoadingSkeletonComponent,
    ErrorStateComponent,
    EmptyStateComponent,
    StatusTagComponent,
    ColleagueInviteDialogComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './colleagues.html',
  styleUrl: './colleagues.scss',
})
export class TeacherColleaguesPage {
  private readonly colleagues = inject(TeacherColleagueService);

  protected readonly search = signal('');
  protected readonly status = signal<AccountStatus | null>(null);
  private readonly query = signal<PageQuery>({ page: 0, size: 20 });

  protected readonly inviteVisible = signal(false);

  protected readonly statusOptions = [
    { value: null, label: 'Todas as situações' },
    ...STATUSES.map((value) => ({ value, label: ACCOUNT_STATUS_LABELS[value] })),
  ];

  protected readonly state = createPageState(() =>
    this.colleagues.list(this.query(), this.search().trim(), this.status()),
  );

  private searchTimer: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.searchTimer));
    void this.state.load();
  }

  protected onSearch(value: string): void {
    this.search.set(value);
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => this.reloadFromFirstPage(), SEARCH_DEBOUNCE_MS);
  }

  protected onStatusChange(value: AccountStatus | null): void {
    this.status.set(value);
    this.reloadFromFirstPage();
  }

  protected onPage(event: PaginatorState): void {
    this.query.set({ page: event.page ?? 0, size: event.rows ?? 20 });
    void this.state.refresh();
  }

  protected first(): number {
    const page = this.state.data();
    return page ? page.page * page.size : 0;
  }

  protected async onInvited(): Promise<void> {
    await this.state.refresh();
  }

  private reloadFromFirstPage(): void {
    this.query.update((query) => ({ ...query, page: 0 }));
    void this.state.refresh();
  }
}
