function navega_conveios() {
    window.location.href = "convenios.html";
}

function navega_contrato() {
    window.location.href = "contrato.html";
}

function navega_informacoes() {
    window.location.href = "informacoes.html";
}

function navega_sair() {
    window.location.href = "login.html";
}

function navega_carteira() {
    window.location.href = "carteira.html";
}

// Mensagem push (FCM) — chamado pelo Android via exibirMensagemPush({titulo, mensagem})
function exibirMensagemPush(dados) {
    if (!dados) return;

    let banner = document.getElementById('pushBanner');
    if (!banner) {
        banner = document.createElement('div');
        banner.id = 'pushBanner';
        banner.style.cssText = 'position:fixed;top:0;left:0;right:0;z-index:9999;background:#1a237e;color:#fff;' +
            'padding:14px 16px;display:none;justify-content:space-between;align-items:flex-start;gap:12px;' +
            'box-shadow:0 4px 12px rgba(0,0,0,0.25);font-family:Segoe UI,sans-serif;';
        const conteudo = document.createElement('div');
        conteudo.style.cssText = 'flex:1;min-width:0;';
        const fechar = document.createElement('span');
        fechar.textContent = '✕';
        fechar.style.cssText = 'cursor:pointer;font-size:16px;opacity:0.9;padding:2px 4px;';
        fechar.onclick = function () { banner.style.display = 'none'; };
        banner.appendChild(conteudo);
        banner.appendChild(fechar);
        document.body.appendChild(banner);
        banner._conteudo = conteudo;
    }

    const titulo = dados.titulo || 'Aviso';
    const mensagem = dados.mensagem || '';
    banner._conteudo.innerHTML =
        '<strong style="display:block;font-size:14px;margin-bottom:3px;">' + escHtml(titulo) + '</strong>' +
        '<span style="font-size:13px;opacity:0.95;">' + escHtml(mensagem) + '</span>';

    banner.style.display = 'flex';
    clearTimeout(banner._timer);
    banner._timer = setTimeout(function () { banner.style.display = 'none'; }, 8000);
}

function escHtml(texto) {
    return String(texto)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
}
