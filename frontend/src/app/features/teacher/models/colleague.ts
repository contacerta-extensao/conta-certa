import type { AccountStatus } from '../../../core/models/enums';

/** Professores da mesma instituição — `GET /teacher/colleagues`. */
export interface TeacherColleague {
  id: string;
  fullName: string;
  email: string;
  registrationNumber: string | null;
  status: AccountStatus;
  createdAt: string;
}

/**
 * `POST /teacher/colleagues`.
 *
 * Sem senha e sem instituição: o colega entra `PENDING` na instituição de quem
 * convida e define a própria senha pelo link do e-mail.
 */
export interface InviteColleagueRequest {
  fullName: string;
  email: string;
  registrationNumber: string;
}
