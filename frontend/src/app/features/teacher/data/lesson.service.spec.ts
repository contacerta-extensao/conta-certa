import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { LessonService } from './lesson.service';

describe('LessonService', () => {
  it('envia PNG sem MIME do navegador como image/png', () => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    const lessons = TestBed.inject(LessonService);
    const http = TestBed.inject(HttpTestingController);
    const file = new File([new Uint8Array([0x89, 0x50, 0x4e, 0x47])], 'grafico.png');

    lessons.uploadImage('lesson-1', file).subscribe();

    const request = http.expectOne('/api/v1/teacher/lessons/lesson-1/images');
    const uploaded = (request.request.body as FormData).get('file') as File;
    expect(uploaded.name).toBe('grafico.png');
    expect(uploaded.type).toBe('image/png');
    request.flush({ id: 'image-1', url: '/api/v1/lesson-images/image-1', fileName: 'grafico.png' });
    http.verify();
  });
});
