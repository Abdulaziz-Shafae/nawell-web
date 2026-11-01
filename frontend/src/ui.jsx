import React, { createContext, useContext, useEffect, useRef, useState } from 'react';
import { api,uploadFormImages } from './api';
import ImageField from './ImageField';
import { riyadhDate } from './rules';
import { ArrowUpRight, Check, LoaderCircle, X, AlertCircle ,Coins,Asterisk,Star,ArrowRight,ArrowLeftRight} from 'lucide-react';
export const AppContext = createContext(null);
export const useApp = () => useContext(AppContext);
export function useResource(loader, deps = []) {
 const [state,set] = useState({ loading:true, data:null, error:null });
 const [version, bump] = useState(0);
 useEffect(() => { let alive = true; const controller = new AbortController(); set(s => ({...s,loading:true,error:null}));
  Promise.resolve().then(() => loader(controller.signal)).then(data => { if(alive) set({data,loading:false,error:null}); }).catch(error => { if(alive && error.name !== 'AbortError') set({data:null,loading:false,error}); });
  return () => { alive = false; controller.abort(); };
 }, [...deps,version]);
 return {...state, reload: () => bump(v => v+1)};
}
export function Button({ children, busy, variant = 'primary', ...props }) { return <button className={`btn ${variant}`} disabled={busy || props.disabled} {...props}>{busy ? <LoaderCircle size={17} className="spin"/> : null}{children}</button>; }
export function Header({ eyebrow, title, subtitle, children }) { return <div className="page-heading"><div><div className="eyebrow">{eyebrow}</div><h1>{title}</h1>{subtitle && <p>{subtitle}</p>}</div><div className="actions">{children}</div></div>; }
export function Badge({ children }) { const {t} = useApp(); return <span className={`badge ${String(children).toLowerCase()}`}>{t(children)}</span>; }
export function Empty({ title, text, children }) { const {t} = useApp(); return <div className="empty"><span className="empty-icon"><ArrowLeftRight aria-hidden="true"/></span><h3>{title || t('Nothing here yet')}</h3><p>{text || t('Your next exchange starts with a little curiosity.')}</p>{children}</div>; }
export function Resource({ state, children, empty }) { const {t} = useApp(); if(state.loading) return <div className="loading" role="status"><LoaderCircle className="spin"/>{t('Loading…')}</div>; if(state.error) return <div className="error-panel" role="alert"><AlertCircle/><div><h3>{t('Could not load this page')}</h3><p>{state.error.message}</p><Button variant="secondary" onClick={state.reload}>{t('Try again')}</Button></div></div>; if(Array.isArray(state.data) && !state.data.length) return empty || <Empty/>; return children(state.data); }
export function Field({ field, value, onChange }) {
 const {t} = useApp(); const id = React.useId(); if(field.type==='image') return <ImageField field={field} value={value} onChange={onChange} id={id}/>; const props = { id, name:field.name, value:value ?? '', onChange:e => onChange(field.type === 'number' ? (e.target.value === '' ? '' : Number(e.target.value)) : e.target.value), required:field.required, min:field.min, max:field.max, maxLength:field.maxLength, minLength:field.minLength, pattern:field.pattern, placeholder:field.placeholder, disabled:field.disabled };
 return <label className={`field ${field.wide ? 'wide' : ''}`} htmlFor={id}><span>{t(field.label || field.name)}{field.required && <b> *</b>}</span>{field.type === 'textarea' ? <textarea {...props} rows={field.rows || 4}/> : field.options ? <select {...props}><option value="">{t('Select…')}</option>{field.options.map(o => <option key={o.value ?? o} value={o.value ?? o}>{t(o.label ?? o)}</option>)}</select> : field.type === 'checkbox' ? <input id={id} type="checkbox" checked={!!value} onChange={e=>onChange(e.target.checked)}/> : <input {...props} type={field.type || 'text'}/>} {field.hint && <small>{t(field.hint)}</small>}</label>;
}
export function Form({ fields, initial = {}, submit, label = 'Save', after }) {
 const {t, toast} = useApp(); const [values, set] = useState(initial); const [busy, setBusy] = useState(false); const [error,setError] = useState('');
 return <form onSubmit={async e => { e.preventDefault(); setBusy(true); setError(''); try { const body = Object.fromEntries(fields.filter(field=>values[field.name] !== undefined).map(field=>[field.name,values[field.name]])); const result = await submit(await uploadFormImages(body)); toast(result?.message?t(result.message):t('Saved successfully')); await after?.(result); } catch(err) {setError(err.message);} finally {setBusy(false);} }}><div className="form-grid">{fields.map(f => <Field key={f.name} field={f} value={values[f.name]} onChange={value => set(v=>({...v,[f.name]:value}))}/>)}</div>{error && <p className="form-error" role="alert">{error}</p>}<div className="form-footer"><Button busy={busy} type="submit">{t(label)}<ArrowUpRight size={17}/></Button></div></form>;
}
export function Action({ action, params, body, children, confirm, done, variant = 'secondary', disabled }) {
 const {t, toast, dialog} = useApp(); const [busy,setBusy] = useState(false);
 const run = async () => { setBusy(true); try { const result = await api(action,params, {body}); toast(result?.message || t('Saved successfully')); await done?.(result); } catch(e) {toast(e.message,'error');} finally {setBusy(false);} };
 return <Button variant={variant} busy={busy} disabled={disabled} onClick={() => confirm ? dialog({title:t('Confirm action'),text:confirm,onConfirm:run}) : run()}>{children}</Button>;
}
export function ModalHost({ modal, close }) {
 const {t} = useApp(); const ref = useRef();
 useEffect(()=> { if(modal) { ref.current.showModal(); } else if(ref.current?.open) ref.current.close(); },[modal]);
 return <dialog className="modal" ref={ref} onCancel={e=> {e.preventDefault();close();}} onClick={e=> {if(e.target === ref.current) close();}}><div className="modal-head"><h2>{modal?.title}</h2><button className="icon-btn" aria-label={t('Close')} onClick={close}><X/></button></div>{modal?.text && <p>{modal.text}</p>}{modal?.fields && <Form key={modal.key} fields={modal.fields} initial={modal.initial} label={modal.label} submit={modal.submit} after={async result=> {close();await modal.after?.(result);}}/>}{modal?.content}{modal?.onConfirm && <div className="form-footer"><Button variant="secondary" onClick={close}>{t('Cancel')}</Button><Button variant="danger" onClick={async()=>{close();await modal.onConfirm();}}>{t('Confirm')}</Button></div>}</dialog>;
}
export function Result({ data }) {
 const {t} = useApp(); if(data == null) return <Empty/>;
 if(typeof data !== 'object') return <p className="result-text">{String(data)}</p>;
 if(Array.isArray(data)) return data.length ? <div className="result-list">{data.map((item,i) => <div key={item?.id ?? i}><Result data={item}/></div>)}</div> : <p className="muted">{t('Nothing here yet')}</p>;
 return <dl className="result">{Object.entries(data).filter(([k])=>!['password','emailVerificationHash','emailVerificationExpiresAt','emailVerificationSentAt'].includes(k)).map(([key,value])=><div key={key}><dt>{t(key.replace(/([A-Z])/g,' $1').replace(/^./,c=>c.toUpperCase()))}</dt><dd>{typeof value === 'boolean' ? t(value ? 'Yes' : 'No') : typeof value === 'object' && value !== null ? <Result data={value}/> : <span style={{whiteSpace:'pre-wrap'}}>{value == null ? '—' : String(value)}</span>}</dd></div>)}</dl>;
}
export function Stat({ label, value, note }) { return <article className="stat"><span>{label}</span><strong>{value ?? '—'}</strong>{note && <small>{note}</small>}</article>; }
export function DateText({ value }) { const {lang} = useApp(); if(!value) return <>—</>; const date = riyadhDate(value); return <time dateTime={value}>{isNaN(date) ? value : date.toLocaleString(lang==='ar'?'ar-SA-u-ca-gregory':'en-GB',{dateStyle:'medium',timeStyle:'short',timeZone:'Asia/Riyadh'})}</time>; }
export const f = (name, options = {}) => ({ name, label:name.replace(/([A-Z])/g,' $1').replace(/^./,c=>c.toUpperCase()),...options });
export const modes = ['ONLINE','IN_PERSON','BOTH'];
export const profileFields = company => [f('name',{required:true,maxLength:company?150:100}),f('phone',{required:true,pattern:'05[0-9]{8}',maxLength:10,hint:'10 digits, starting with 05'}),f('city',{maxLength:100}),f(company?'logo':'profileImage',{type:'image'}),f(company?'description':'bio',{type:'textarea',maxLength:500,wide:true})];
export const offerFields = [f('description',{type:'textarea',maxLength:500,wide:true}),f('mode',{required:true,options:modes}),f('tokenCost',{required:true,type:'number',min:1}),f('capacity',{required:true,type:'number',min:1})];
export const sessionFields = [f('title',{required:true,maxLength:150,wide:true}),f('scheduledAt',{type:'datetime-local',required:true,hint:'Dates use Asia/Riyadh time'}),f('durationMinutes',{type:'number',required:true,min:1}),f('mode',{required:true,options:['ONLINE','IN_PERSON']}),f('location'),f('meetingLink',{type:'url',maxLength:2000})];

export function RatingStars({rating}) {return <span className="rating-stars" role="img" aria-label={`${rating} / 5`}>{Array.from({length:5},(_,index)=><Star key={index} size={17} aria-hidden="true" fill={index<rating?'currentColor':'none'}/>)}</span>;}
