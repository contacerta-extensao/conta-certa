package com.ifsc.contacerta.repository;

import java.util.UUID;

/** Contagem agregada por lição, para não consultar uma lição por vez na listagem. */
public interface LessonCountProjection {

	UUID getLessonId();

	long getTotal();
}
