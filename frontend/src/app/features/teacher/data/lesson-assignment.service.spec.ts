import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { LessonAssignmentService } from './lesson-assignment.service';

describe('LessonAssignmentService', () => {
  let assignments: LessonAssignmentService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()],
    });
    assignments = TestBed.inject(LessonAssignmentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envia os IDs e as versões atuais ao reordenar a trilha', () => {
    void assignments.reorder('room-1', {
      assignments: [
        { assignmentId: 'assignment-2', version: 4 },
        { assignmentId: 'assignment-1', version: 7 },
      ],
    });

    const request = http.expectOne('/api/v1/teacher/rooms/room-1/lesson-assignments/order');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({
      assignments: [
        { assignmentId: 'assignment-2', version: 4 },
        { assignmentId: 'assignment-1', version: 7 },
      ],
    });
    request.flush([]);
  });

  it('envia a versão na query ao retirar uma lição da trilha', () => {
    void assignments.remove('room-1', 'assignment-1', 9);

    const request = http.expectOne(
      '/api/v1/teacher/rooms/room-1/lesson-assignments/assignment-1?version=9',
    );
    expect(request.request.method).toBe('DELETE');
    expect(request.request.body).toBeNull();
    request.flush(null);
  });
});
