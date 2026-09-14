import { environment } from './environment.real';

describe('environment real', () => {
  it('desativa o mock e mantém o contrato da API', () => {
    expect(environment.production).toBe(false);
    expect(environment.apiBaseUrl).toBe('/api/v1');
    expect(environment.useMockApi).toBe(false);
    expect(environment.presentationTimeZone).toBe('America/Sao_Paulo');
  });
});
