export class ApiError extends Error {
 constructor(message, status, payload) { super(message); this.status = status; this.payload = payload; }
}
export const routes = {
 uploadImage: ['POST','/api/v1/media/images'], image: ['GET','/api/v1/media/images/{name}'],
 profile: ['GET','/api/v1/account/profile'], dashboard: ['GET','/api/v1/account/dashboard'], login: ['POST','/api/v1/account/login'], logout: ['POST','/api/v1/account/logout'], registerIndividual: ['POST','/api/v1/account/register/individual'], registerCompany: ['POST','/api/v1/account/register/company'], sendVerification: ['POST','/api/v1/account/send-verification-email'], verifyEmail: ['GET','/api/v1/account/verify-email'],
 skills: ['GET','/api/v1/skill/get'], mySkills: ['GET','/api/v1/account-skill/account/{accountId}'], verifiedSkills: ['GET','/api/v1/account-skill/verified/{accountId}'], addSkill: ['POST','/api/v1/account-skill/add/{skillId}'], removeSkill: ['DELETE','/api/v1/account-skill/delete/{id}'],
 offers: ['GET','/api/v1/skill-offer/available'], skillOffers: ['GET','/api/v1/skill-offer/skill/{skillId}'], providerOffers: ['GET','/api/v1/skill-offer/provider/{providerId}'], createOffer: ['POST','/api/v1/skill-offer/create/{skillId}'], evaluateOffer: ['POST','/api/v1/skill-offer/create/{skillId}/evaluate'], updateOffer: ['PUT','/api/v1/skill-offer/update/{id}'], deleteOffer: ['DELETE','/api/v1/skill-offer/delete/{id}'],
 sentRequests: ['GET','/api/v1/learning-request/requester/{accountId}'], receivedRequests: ['GET','/api/v1/learning-request/provider/{accountId}'], openRequests: ['GET','/api/v1/learning-request/open'], urgentRequests: ['GET','/api/v1/learning-request/urgent'], skillRequests: ['GET','/api/v1/learning-request/skill/{skillId}'], request: ['GET','/api/v1/learning-request/{requestId}'], createRequest: ['POST','/api/v1/learning-request/create/{offerId}'], updateRequest: ['PUT','/api/v1/learning-request/update/{id}'], deleteRequest: ['DELETE','/api/v1/learning-request/delete/{id}'], cancelRequest: ['PUT','/api/v1/learning-request/{requestId}/cancel'],
 respond: ['POST','/api/v1/request-negotiation/respond/{requestId}'], negotiations: ['GET','/api/v1/request-negotiation/request/{requestId}'], latestNegotiation: ['GET','/api/v1/request-negotiation/latest/{requestId}'], proposal: ['GET','/api/v1/request-negotiation/{requestId}/calculate-urgency/{negotiationId}'], acceptProposal: ['PUT','/api/v1/request-negotiation/{negotiationId}/accept'],
 exchanges: ['GET','/api/v1/exchange/account'], exchange: ['GET','/api/v1/exchange/{exchangeId}'], createExchange: ['POST','/api/v1/exchange/create/{requestId}/{offerId}'], acceptExchange: ['PUT','/api/v1/exchange/{exchangeId}/accept'], cancelExchange: ['PUT','/api/v1/exchange/{exchangeId}/cancel'], completeExchange: ['PUT','/api/v1/exchange/{exchangeId}/complete'],
 offerSessions: ['GET','/api/v1/session/offer/{offerId}'], exchangeSessions: ['GET','/api/v1/session/exchange/{exchangeId}'], createSession: ['POST','/api/v1/session/create/{offerId}'], updateSession: ['PUT','/api/v1/session/update/{id}'], deleteSession: ['DELETE','/api/v1/session/delete/{id}'], joinSession: ['POST','/api/v1/session/{sessionId}/join/{exchangeId}'], attendance: ['PUT','/api/v1/session/{sessionId}/attendance/{exchangeId}'],
 agreementStatus: ['GET','/api/v1/agreement/{exchangeId}/status'], providerAccept: ['PUT','/api/v1/agreement/{exchangeId}/provider-accept'], receiverAccept: ['PUT','/api/v1/agreement/{exchangeId}/receiver-accept'], addAgreement: ['POST','/api/v1/agreement/add'], updateAgreement: ['PUT','/api/v1/agreement/update/{id}'],
 assessmentHistory: ['GET','/api/v1/skill-assessment/history/{accountSkillId}'], latestAssessment: ['GET','/api/v1/skill-assessment/latest/{accountSkillId}'], takeAssessment: ['POST','/api/v1/skill-assessment/take/{accountSkillId}'], generateAssessment: ['POST','/api/v1/ai/assessment/generate/{accountSkillId}'], evaluateAssessment: ['POST','/api/v1/ai/assessment/evaluate/{accountSkillId}'],
 balance: ['GET','/api/v1/token-transaction/account/balance'], history: ['GET','/api/v1/token-transaction/account/history'], bonus: ['POST','/api/v1/token-transaction/bonus'], refund: ['POST','/api/v1/token-transaction/refund/{exchangeId}'], purchase: ['POST','/api/v1/token-transaction/purchase/{amount}'], redeem: ['POST','/api/v1/token-transaction/redeem/{amount}'],
 reviews: ['GET','/api/v1/review/account/{accountId}'], rating: ['GET','/api/v1/review/account/{accountId}/average'], review: ['POST','/api/v1/review/add/{exchangeId}'], providers: ['GET','/api/v1/search/providers/{skillId}'], searchRequests: ['GET','/api/v1/search/requests/{skillId}'],
 match: ['GET','/api/v1/ai/match/{skillId}'], matchExplanation: ['GET','/api/v1/ai/match/{providerId}/{skillId}/explanation'], extractCv: ['POST','/api/v1/ai/cv/extract-skills'], suggestions: ['GET','/api/v1/ai/cv/suggest-offers'], fairness: ['POST','/api/v1/ai/exchange/fairness/{requestId}/{offerId}'], generateAgreement: ['POST','/api/v1/ai/agreement/generate/{exchangeId}'], linkedin: ['POST','/api/v1/ai/linkedin/get-skills'], addLinkedin: ['POST','/api/v1/ai/linkedin/add-skills'], relationships: ['GET','/api/v1/ai/skill/relationships/{skillId}'], relatedProviders: ['GET','/api/v1/ai/skill/{skillId}/related-providers'],
 updateIndividual: ['PUT','/api/v1/individual-profile/update/{id}'], updateCompany: ['PUT','/api/v1/company-profile/update/{id}'],
};
export function routePath(template, params = {}) {
 return template.replace(/\{(\w+)\}/g, (_, key) => {
  if (params[key] === undefined || params[key] === null || params[key] === '') throw new Error(`Missing ${key}`);
  return encodeURIComponent(params[key]);
 });
}
export async function request(method, path, { body, query, signal } = {}) {
 const base = import.meta.env?.VITE_API_BASE_URL || '';
 const suffix = query ? '?' + new URLSearchParams(query) : '';
 const multipart = typeof FormData !== 'undefined' && body instanceof FormData;
 let response;
 try {
  response = await fetch(base.replace(/\/$/, '') + path + suffix, { method, credentials: 'include', signal, headers: { 'X-Requested-With': 'XMLHttpRequest', ...(body !== undefined && !multipart ? { 'Content-Type': 'application/json' } : {}) }, body: body === undefined ? undefined : multipart ? body : JSON.stringify(body) });
 } catch (error) { if (error.name === 'AbortError') throw error; throw new ApiError('Could not connect to the server. Please try again.', 0); }
 const text = await response.text();
 let data; try { data = text ? JSON.parse(text) : null; } catch { throw new ApiError('The server returned an unexpected response.', response.status); }
 if (!response.ok) {
  if (response.status === 401) globalThis.dispatchEvent?.(new Event('nawall:unauthorized'));
  throw new ApiError(data?.message || data?.error || (response.status>=500 ? 'The server is unavailable. Please try again later.' : 'Request failed. Please try again.'), response.status, data);
 }
 return data;
}
export function api(key, params, options) { const route = routes[key]; if (!route) throw new Error(`Unknown action ${key}`); return request(route[0], routePath(route[1], params), options); }

export async function uploadFormImages(body) {
 const prepared={...body};
 for(const name of ['profileImage','logo']) {
  if(typeof File!=='undefined' && prepared[name] instanceof File) {
   const form=new FormData();form.append('file',prepared[name]);
   const result=await api('uploadImage',{}, {body:form});
   if(!result?.url)throw new ApiError('The image upload did not return a saved image URL',502,result);
   prepared[name]=result.url;
  }
 }
 return prepared;
}
