import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiClient } from '../../../core/api/api-client';
import type { AccountStatus } from '../../../core/models/enums';
import type { Page, PageQuery } from '../../../core/models/page';
import type { InviteColleagueRequest, TeacherColleague } from '../models/colleague';

/** Professores da instituição do professor autenticado. */
@Injectable({ providedIn: 'root' })
export class TeacherColleagueService {
  private readonly api = inject(ApiClient);
  private readonly base = '/teacher/colleagues';

  list(
    query: PageQuery = {},
    search = '',
    status: AccountStatus | null = null,
  ): Promise<Page<TeacherColleague>> {
    return firstValueFrom(
      this.api.getPage<TeacherColleague>(this.base, query, {
        params: { search, status },
      }),
    );
  }

  /** Cria a conta em `PENDING` e envia o convite por e-mail. */
  invite(body: InviteColleagueRequest): Promise<TeacherColleague> {
    return firstValueFrom(this.api.post<TeacherColleague>(this.base, body));
  }
}
