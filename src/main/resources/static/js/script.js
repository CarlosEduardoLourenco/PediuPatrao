document.querySelector('.toggle-btn').addEventListener('click', () => {
    document.getElementById('sidebar').classList.toggle('active');
});

function toggleSubmenu(event, element) {
    event.preventDefault();
    element.parentElement.classList.toggle('open');
}

function cabecalhosDeEscrita() {
    const token = document.querySelector('meta[name="_csrf"]').content;
    const nomeDoCabecalho = document.querySelector('meta[name="_csrf_header"]').content;
    return { [nomeDoCabecalho]: token };
}

async function enviarJson(url, corpo, metodo = 'POST') {
    const resposta = await fetch(url, {
        method: metodo,
        headers: { 'Content-Type': 'application/json', ...cabecalhosDeEscrita() },
        body: JSON.stringify(corpo ?? {})
    });
    if (resposta.ok) {
        return resposta.json();
    }
    let mensagem = 'Não foi possível concluir a operação.';
    try {
        const erro = await resposta.json();
        mensagem = erro.mensagem || mensagem;
    } catch (corpoSemJson) {
        mensagem = `Operação recusada (${resposta.status}).`;
    }
    throw new Error(mensagem);
}

function exibirErro(mensagem) {
    const area = document.getElementById('mensagemAcao');
    area.textContent = mensagem;
    area.classList.remove('d-none');
    window.scrollTo({ top: 0, behavior: 'smooth' });
}
