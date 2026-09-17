export const environment = {
  production: false,
  /** Base URL da API. Ver §2.1 da spec de integração. */
  apiBaseUrl: '/api/v1',
  /** Fuso de apresentação. A API sempre trafega UTC. */
  presentationTimeZone: 'America/Sao_Paulo',
} as const;
