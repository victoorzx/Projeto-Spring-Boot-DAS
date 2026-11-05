const $=s=>document.querySelector(s);
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const STATUS={ABERTO:'Aberto',EM_ATENDIMENTO:'Em atendimento',AGUARDANDO_CLIENTE:'Aguardando cliente',FECHADO:'Fechado'};
const PRIO={BAIXA:'Baixa',MEDIA:'Média',ALTA:'Alta',CRITICA:'Crítica'};
const NEXT={ABERTO:['EM_ATENDIMENTO','FECHADO'],EM_ATENDIMENTO:['AGUARDANDO_CLIENTE','FECHADO'],AGUARDANDO_CLIENTE:['EM_ATENDIMENTO','FECHADO'],FECHADO:[]};
const CRUD={
 clientes:{t:'Clientes',s:'o cliente',c:[['nome','Nome'],['email','E-mail','email'],['telefone','Telefone']],obr:['nome','email']},
 categorias:{t:'Categorias',s:'a categoria',c:[['nome','Nome'],['descricao','Descrição']],obr:['nome']},
 tecnicos:{t:'Técnicos',s:'o técnico',c:[['nome','Nome'],['email','E-mail','email'],['perfil','Perfil','select']],obr:['nome','email','perfil']}
};
const tag=(k,v)=>`<span class="tag ${k}-${v}">${(k==='s'?STATUS:PRIO)[v]||esc(v)}</span>`;
const dt=s=>s?new Date(s).toLocaleString('pt-BR',{dateStyle:'short',timeStyle:'short'}):'';
const lg=k=>{try{return localStorage.getItem(k)||''}catch(e){return ''}};
const sv=(k,v)=>{try{localStorage.setItem(k,v)}catch(e){}};

async function api(path,opt={}){
 const r=await fetch(path,{...opt,headers:{'Content-Type':'application/json'},body:opt.body?JSON.stringify(opt.body):undefined});
 if(r.status===204)return null;
 let d=null;try{d=await r.json()}catch(e){}
 if(!r.ok){const e=new Error(d&&d.erro?d.erro:'Erro inesperado ('+r.status+')');e.detalhes=d&&d.detalhes||[];throw e}
 return d;
}
function aviso(msg,erro){const t=$('#aviso');t.textContent=msg;t.className=erro?'erro':'';t.hidden=false;clearTimeout(aviso.t);aviso.t=setTimeout(()=>{t.hidden=true},6000)}
const falha=e=>aviso([e.message,...(e.detalhes||[])].join(' · '),true);

const f={status:'',prioridade:'',page:0};
async function telaLista(){
 const nomes=Object.keys(STATUS);
 const q=new URLSearchParams({page:f.page,size:10});
 if(f.status)q.set('status',f.status);
 if(f.prioridade)q.set('prioridade',f.prioridade);
 const [pg,...tot]=await Promise.all([api('/chamados?'+q),...nomes.map(s=>api('/chamados?status='+s+'&size=1'))]);
 const n=Object.fromEntries(nomes.map((s,i)=>[s,tot[i].totalElementos]));
 const soma=Object.values(n).reduce((a,b)=>a+b,0);
 const chip=(v,l,c)=>`<button class="chip${f.status===v?' on':''}" data-s="${v}">${l}${c?`<span>${c}</span>`:''}</button>`;
 const sc=!f.status;
 const lin=pg.conteudo.map(c=>`<tr><td>#${c.id}</td><td><a href="#/chamados/${c.id}">${esc(c.titulo)}</a></td><td>${esc(c.clienteNome)}</td><td>${esc(c.categoriaNome)}</td><td>${tag('p',c.prioridade)}</td>${sc?`<td>${tag('s',c.status)}</td>`:''}<td>${dt(c.dataAbertura)}</td></tr>`).join('');
 const filtrado=f.status||f.prioridade;
 const corpo=pg.conteudo.length
  ?`<div class="rolagem"><table><thead><tr><th>Nº</th><th>Título</th><th>Cliente</th><th>Categoria</th><th>Prioridade</th>${sc?'<th>Status</th>':''}<th>Aberto em</th></tr></thead><tbody>${lin}</tbody></table></div>`
  :filtrado?`<p class="vazio">Nenhum chamado com esses filtros. <button class="ghost" id="limpar">Limpar filtros</button></p>`
  :`<p class="vazio">Nenhum chamado ainda. <a href="#/chamados/novo">Abrir o primeiro chamado</a>.</p>`;
 const pag=pg.totalPaginas>1?`<div class="acoes pag">${f.page>0?'<button id="ant">← Anteriores</button>':''}<span class="muted">Página ${pg.pagina+1} de ${pg.totalPaginas}</span>${f.page<pg.totalPaginas-1?'<button id="prox">Próximos →</button>':''}</div>`:'';
 $('#view').innerHTML=`<div class="topo"><h1>Chamados</h1><a class="primario" href="#/chamados/novo">Novo chamado</a></div>
 <div class="chips">${chip('','Todos',soma)}${nomes.map(s=>chip(s,STATUS[s],n[s])).join('')}</div>
 <div class="filtros"><select id="prio" aria-label="Prioridade"><option value="">Todas as prioridades</option>${Object.entries(PRIO).map(([v,l])=>`<option value="${v}"${f.prioridade===v?' selected':''}>${l}</option>`).join('')}</select><span class="muted">Ordenados do mais recente para o mais antigo</span></div>
 ${corpo}${pag}`;
 document.querySelectorAll('.chip').forEach(b=>b.onclick=()=>{f.status=b.dataset.s;f.page=0;telaLista()});
 $('#prio').onchange=e=>{f.prioridade=e.target.value;f.page=0;telaLista()};
 if($('#limpar'))$('#limpar').onclick=()=>{f.status='';f.prioridade='';f.page=0;telaLista()};
 if($('#ant'))$('#ant').onclick=()=>{f.page--;telaLista()};
 if($('#prox'))$('#prox').onclick=()=>{f.page++;telaLista()};
}

async function telaNovo(){
 const [cl,ca,te]=await Promise.all([api('/clientes'),api('/categorias'),api('/tecnicos')]);
 if(!cl.length||!ca.length){
  const falta=[!cl.length&&'<a href="#/clientes">um cliente</a>',!ca.length&&'<a href="#/categorias">uma categoria</a>'].filter(Boolean).join(' e ');
  $('#view').innerHTML=`<h1>Novo chamado</h1><p class="vazio">Para abrir um chamado é preciso ter ${falta} cadastrado.</p>`;
  return;
 }
 const op=(a,k)=>a.map(x=>`<option value="${x.id}">${esc(x[k])}</option>`).join('');
 const campos={descricao:'<label>Descrição<textarea name="descricao" maxlength="2000" rows="4"></textarea></label>',tecnicoId:`<label>Técnico<select name="tecnicoId">${op(te,'nome')}</select></label>`};
 $('#view').innerHTML=`<h1>Novo chamado</h1><form id="form" class="form" style="margin-top:16px">
 <label>Título<input name="titulo" required maxlength="150"></label>
 <label>Cliente<select name="clienteId" required>${op(cl,'nome')}</select></label>
 <label>Categoria<select name="categoriaId" required>${op(ca,'nome')}</select></label>
 <label>Prioridade<select name="prioridade">${Object.entries(PRIO).map(([v,l])=>`<option value="${v}"${v==='MEDIA'?' selected':''}>${l}</option>`).join('')}</select></label>
 <div id="opc"><button type="button" class="ghost" data-c="descricao">+ Descrição</button>${te.length?'<button type="button" class="ghost" data-c="tecnicoId">+ Técnico</button>':''}</div>
 <div class="acoes"><button class="primario">Abrir chamado</button><a href="#/chamados">Cancelar</a></div></form>`;
 const form=$('#form');
 form.elements.titulo.focus();
 $('#opc').onclick=e=>{const c=e.target.dataset.c;if(!c)return;$('#opc').insertAdjacentHTML('beforebegin',campos[c]);e.target.remove();form.elements[c].focus()};
 form.onsubmit=async e=>{
  e.preventDefault();
  const d=Object.fromEntries(new FormData(form));
  ['clienteId','categoriaId','tecnicoId'].forEach(k=>{if(d[k])d[k]=Number(d[k]);else delete d[k]});
  if(!d.descricao)delete d.descricao;
  const b=e.submitter;b.disabled=true;
  try{const r=await api('/chamados',{method:'POST',body:d});location.hash='#/chamados/'+r.id}catch(x){falha(x);b.disabled=false}
 };
}

async function telaDetalhe(id){
 const [d,te]=await Promise.all([api('/chamados/'+id),api('/tecnicos')]);
 const c=d.chamado,fechado=c.status==='FECHADO';
 const prox=NEXT[c.status].map(s=>s==='FECHADO'?`<button class="perigo" data-s="${s}">Fechar chamado</button>`:`<button data-s="${s}">Mover para ${STATUS[s].toLowerCase()}</button>`).join('');
 const atrib=te.length
  ?`<select id="tec" aria-label="Técnico">${c.tecnicoId?'':'<option value="">Selecione o técnico</option>'}${te.map(t=>`<option value="${t.id}"${t.id===c.tecnicoId?' selected':''}>${esc(t.nome)}</option>`).join('')}</select><button id="atr">Atribuir</button>`
  :'<span class="muted">Nenhum técnico cadastrado. <a href="#/tecnicos">Cadastrar técnico</a></span>';
 const coms=d.comentarios.length
  ?d.comentarios.map(m=>`<div class="com"><div class="muted">${esc(m.autor)} · ${dt(m.data)}</div>${esc(m.texto)}</div>`).join('')
  :'<p class="vazio">Sem comentários ainda.</p>';
 $('#view').innerHTML=`<p class="muted"><a href="#/chamados">← Chamados</a></p>
 <div class="topo"><h1>#${c.id} ${esc(c.titulo)}</h1><div class="acoes">${tag('s',c.status)}${tag('p',c.prioridade)}</div></div>
 <section class="ficha"><div class="meta">
  <div><div class="muted">Cliente</div>${esc(c.clienteNome)}</div>
  <div><div class="muted">Categoria</div>${esc(c.categoriaNome)}</div>
  <div><div class="muted">Técnico</div>${c.tecnicoNome?esc(c.tecnicoNome):'Sem técnico'}</div>
  <div><div class="muted">Aberto em</div>${dt(c.dataAbertura)}</div>
  ${c.dataFechamento?`<div><div class="muted">Fechado em</div>${dt(c.dataFechamento)}</div>`:''}
 </div>${c.descricao?`<p>${esc(c.descricao)}</p>`:''}</section>
 ${fechado?'<p class="muted">Chamado fechado: não pode mais ser alterado, mas aceita comentários.</p>':`<section class="ficha"><div class="acoes">${prox}</div><div class="acoes" style="margin-top:12px">${atrib}</div></section>`}
 <h2>Comentários</h2>${coms}
 <form id="com" class="form" style="margin-top:12px"><label>Autor<input name="autor" required maxlength="100" value="${esc(lg('autor'))}"></label><label>Comentário<textarea name="texto" required rows="3" maxlength="1000"></textarea></label><div class="acoes"><button class="primario">Comentar</button></div></form>`;
 document.querySelectorAll('[data-s]').forEach(b=>b.onclick=async()=>{
  const s=b.dataset.s;
  if(s==='FECHADO'&&!confirm('Fechar o chamado #'+c.id+'? Depois de fechado ele não pode mais ser alterado.'))return;
  try{await api('/chamados/'+id+'/status',{method:'PATCH',body:{status:s}});telaDetalhe(id)}catch(e){falha(e)}
 });
 if($('#atr'))$('#atr').onclick=async()=>{
  const v=$('#tec').value;if(!v)return;
  try{await api('/chamados/'+id+'/atribuir',{method:'PATCH',body:{tecnicoId:Number(v)}});telaDetalhe(id)}catch(e){falha(e)}
 };
 $('#com').onsubmit=async e=>{
  e.preventDefault();
  const x=Object.fromEntries(new FormData(e.target));
  try{await api('/chamados/'+id+'/comentarios',{method:'POST',body:x});sv('autor',x.autor);telaDetalhe(id)}catch(err){falha(err)}
 };
}

function campoHtml([n,l,t],v='',req){
 if(t==='select')return `<label>${l}<select name="${n}">${['TECNICO','ADMIN'].map(o=>`<option${o===v?' selected':''}>${o}</option>`).join('')}</select></label>`;
 return `<label>${l}<input name="${n}" type="${t||'text'}" value="${esc(v)}"${req?' required':''}></label>`;
}
function formCrud(cfg,item){
 const v=item||{},ob=cfg.c.filter(c=>cfg.obr.includes(c[0])),op=cfg.c.filter(c=>!cfg.obr.includes(c[0]));
 return `<form class="form">${ob.map(c=>campoHtml(c,v[c[0]],1)).join('')}${op.filter(c=>v[c[0]]).map(c=>campoHtml(c,v[c[0]])).join('')}<div id="opc">${op.filter(c=>!v[c[0]]).map(c=>`<button type="button" class="ghost" data-c="${c[0]}">+ ${c[1]}</button>`).join('')}</div><div class="acoes"><button class="primario">Salvar</button><button type="button" id="canc">Cancelar</button></div></form>`;
}
async function telaCrud(tipo){
 const cfg=CRUD[tipo],lista=await api('/'+tipo);
 const lin=lista.map(x=>`<tr>${cfg.c.map(c=>`<td>${esc(x[c[0]])}</td>`).join('')}<td class="acao"><button data-e="${x.id}">Editar</button> <button class="perigo" data-x="${x.id}">Excluir</button></td></tr>`).join('');
 $('#view').innerHTML=`<div class="topo"><h1>${cfg.t}</h1><button class="primario" id="novo">Novo</button></div><section id="painel" class="ficha" hidden></section>${lista.length?`<div class="rolagem"><table><thead><tr>${cfg.c.map(c=>`<th>${c[1]}</th>`).join('')}<th></th></tr></thead><tbody>${lin}</tbody></table></div>`:'<p class="vazio">Nenhum registro ainda.</p>'}`;
 const painel=$('#painel');
 const abrir=item=>{
  painel.hidden=false;painel.innerHTML=formCrud(cfg,item);
  const form=painel.querySelector('form');
  form.querySelector('input,select').focus();
  $('#opc').onclick=e=>{const n=e.target.dataset.c;if(!n)return;$('#opc').insertAdjacentHTML('beforebegin',campoHtml(cfg.c.find(c=>c[0]===n)));e.target.remove();form.elements[n].focus()};
  $('#canc').onclick=()=>{painel.hidden=true};
  form.onsubmit=async e=>{
   e.preventDefault();
   const d=Object.fromEntries([...new FormData(form)].filter(([,v])=>v!==''));
   try{await api('/'+tipo+(item?'/'+item.id:''),{method:item?'PUT':'POST',body:d});aviso('Salvo.');telaCrud(tipo)}catch(x){falha(x)}
  };
 };
 $('#novo').onclick=()=>abrir();
 document.querySelectorAll('[data-e]').forEach(b=>b.onclick=()=>abrir(lista.find(x=>String(x.id)===b.dataset.e)));
 document.querySelectorAll('[data-x]').forEach(b=>b.onclick=async()=>{
  const x=lista.find(i=>String(i.id)===b.dataset.x);
  if(!confirm('Excluir '+cfg.s+' "'+x.nome+'"? Esta ação não pode ser desfeita.'))return;
  try{await api('/'+tipo+'/'+x.id,{method:'DELETE'});aviso('Excluído.');telaCrud(tipo)}
  catch(e){aviso(/500/.test(e.message)?'Não foi possível excluir: o registro está em uso por chamados.':e.message,true)}
 });
}

const rotas=[
 [/^#\/chamados\/novo$/,telaNovo],
 [/^#\/chamados\/(\d+)$/,telaDetalhe],
 [/^#\/(clientes|categorias|tecnicos)$/,telaCrud],
 [/^(?:#\/chamados\/?|#\/?)?$/,telaLista]
];
async function rota(){
 const h=location.hash;
 document.querySelectorAll('nav a').forEach(a=>a.classList.toggle('on',(h||'#/chamados').startsWith(a.getAttribute('href'))));
 window.scrollTo(0,0);
 for(const [re,fn] of rotas){
  const m=h.match(re);
  if(m){try{await fn(...m.slice(1))}catch(e){$('#view').innerHTML='<p class="vazio">Não foi possível carregar os dados. '+esc(e.message)+'</p>'}return}
 }
 $('#view').innerHTML='<p class="vazio">Página não encontrada.</p>';
}
addEventListener('hashchange',rota);
rota();