import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { NotificationService } from '../../../../core/notifications/notification.service';
import { ReportService } from '../../data/report.service';
import { TeacherRoomService } from '../../data/teacher-room.service';
import { ReportsPage } from './reports';

describe('ReportsPage', () => {
  it('aguarda uma sala antes de consultar os relatórios', async () => {
    let resolveRooms!: (rooms: { id: string; name: string; archived: boolean }[]) => void;
    const rooms = new Promise<{ id: string; name: string; archived: boolean }[]>((resolve) => {
      resolveRooms = resolve;
    });
    const overview = vi.fn().mockResolvedValue({});
    const students = vi.fn().mockResolvedValue({ content: [] });
    const ranking = vi.fn().mockResolvedValue({ content: [] });

    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        { provide: TeacherRoomService, useValue: { options: () => rooms } },
        { provide: ReportService, useValue: { overview, students, ranking } },
        { provide: NotificationService, useValue: {} },
      ],
    });
    TestBed.overrideComponent(ReportsPage, { set: { template: '' } });
    const fixture = TestBed.createComponent(ReportsPage);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(overview).not.toHaveBeenCalled();
    expect(students).not.toHaveBeenCalled();
    expect(ranking).not.toHaveBeenCalled();

    resolveRooms([{ id: 'room-1', name: 'Sala 1', archived: false }]);
    await new Promise((resolve) => setTimeout(resolve, 0));
    await fixture.whenStable();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(overview).toHaveBeenCalledWith(expect.objectContaining({ roomId: 'room-1' }));
    expect(students).toHaveBeenCalledWith(expect.objectContaining({ roomId: 'room-1' }), expect.anything());
    expect(ranking).toHaveBeenCalledWith(expect.objectContaining({ roomId: 'room-1' }));
  });
});
