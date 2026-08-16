import req from '@/utils/request'

// ========== 双色球 ==========
export const ssq = {
  latest:         ()               => req.get('/ssq/draws/latest'),
  byIssue:        (issue)          => req.get(`/ssq/draws/issue/${issue}`),
  list:           (params)         => req.get('/ssq/draws', { params }),
  count:          ()               => req.get('/ssq/draws/count'),
  randomSingle:   ()               => req.get('/ssq/pick/random'),
  randomMulti:    (count)          => req.get(`/ssq/pick/random/${count}`),
  compoundPick:   (payload)        => req.post('/ssq/pick/compound', payload),
  dantuoPick:     (payload)        => req.post('/ssq/pick/dantuo', payload),
  matchSingle:    (payload, issue) => req.post('/ssq/match/single', payload, { params: issue ? { issue } : {} }),
  matchCompound:  (payload, issue) => req.post('/ssq/match/compound', payload, { params: issue ? { issue } : {} }),
  matchDantuo:    (payload, issue) => req.post('/ssq/match/dantuo', payload, { params: issue ? { issue } : {} }),
  frequency:      (recent = 100)   => req.get('/ssq/analysis/frequency', { params: { recent } }),
  statistics:     (recent = 100)   => req.get('/ssq/statistics', { params: { recent } })
}

// ========== 快乐8 ==========
export const kl8 = {
  latest:     ()              => req.get('/kl8/draws/latest'),
  byIssue:    (issue)         => req.get(`/kl8/draws/issue/${issue}`),
  list:       (params)        => req.get('/kl8/draws', { params }),
  count:      ()              => req.get('/kl8/draws/count'),
  frequency:  (recent = 100)  => req.get('/kl8/analysis/frequency', { params: { recent } }),
  statistics: (recent = 100)  => req.get('/kl8/statistics', { params: { recent } })
}

// ========== 投注 ==========
export const bet = {
  save:     (data)   => req.post('/bet', data),
  update:   (data)   => req.put('/bet', data),
  delete:   (id)     => req.delete(`/bet/${id}`),
  get:      (id)     => req.get(`/bet/${id}`),
  list:     (params) => req.get('/bet', { params }),
  match:    (id, issue) => req.post(`/bet/${id}/match`, null, { params: { issue } }),
  matchAll: ()       => req.post('/bet/match-all-ssq')
}

// ========== 系统 ==========
export const sys = {
  health:     () => req.get('/system/health'),
  syncStatus: () => req.get('/system/sync-status'),
  syncSsq:    () => req.post('/system/sync-ssq'),
  syncKl8:    () => req.post('/system/sync-kl8')
}
