import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { TeacherRoomService } from './teacher-room.service';

describe('TeacherRoomService', () => {
  let rooms: TeacherRoomService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()],
    });
    rooms = TestBed.inject(TeacherRoomService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envia a versão atual ao arquivar a sala', () => {
    void rooms.archive('room-1', 3);

    const request = http.expectOne('/api/v1/teacher/rooms/room-1/archive');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ version: 3 });
    request.flush({ id: 'room-1', version: 4, archived: true });
  });
});
