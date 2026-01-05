# ProjetoTCPJava

# Ludo TCP - Jogo em Rede

## Integrantes da Equipe

- Ana Vitoria Araujo Albuquerque 
- Diego Santana Estevão
- Layane De Santana Soares
- Raissa Marta De Santana
- Vivian Ferreira de Melo
  
## Descrição do Jogo

Ludo TCP é uma implementação do jogo clássico Ludo desenvolvida em Java com sockets TCP. O jogo permite que 3 jogadores compitam entre si através de uma conexão de rede.

**Objetivos do Jogo:**
- Cada jogador controla 4 peças
- O objetivo é mover todas as 4 peças até o centro do tabuleiro
- O primeiro jogador a levar todas as peças ao centro vence
- As peças começam na base e só saem com uma jogada de valor 5
- As peças podem capturar peças adversárias, enviando-as de volta à base

## Como Executar o Projeto

### Passos para Compilar

1. Abra o projeto em VS Code
2. Você pode compilar de forma automática
3. Sempre inicie primeiro o LudoTCPServer.java

### Passos para Executar

1. **Inicie o Servidor:**
   - Inicie o arquivo LudoTCPServer.java
   - O servidor aguardará 3 clientes conectarem

2. **Inicie os Clientes (em 3 terminais diferentes):**
   - Vá no arquivo LudoTCPClient.java e execute ele
   - Após cada cliente conectar, digite seu nome
   - Repita o processo 3 vezes para os 3 jogadores

## Como Jogar

### Fluxo do Jogo

1. **Conexão:** Cada cliente se conecta ao servidor e registra seu nome
2. **Turno do Jogador:** Na sua vez:
   - Digite `girar` para rolar o dado (resultado: 1 a 6)
   - Escolha uma peça para mover (0-3) ou digite `skip` se não conseguir mover
   
3. **Movimentação de Peças:**
   - Peças na base só saem com resultado 5
   - Peças no tabuleiro se movem conforme a quantidade rolada
   - Se passar do centro, a peça fica na mesma posição
   - Deve chegar ao centro com o valor exato

4. **Captura:**
   - Se sua peça cair na mesma posição de uma peça adversária, ela é capturada
   - A peça capturada volta à base

5. **Dados:**
   - Se rolar 6, tem direito a outro turno
   - Caso contrário, passa a vez para o próximo jogador

6. **Vitória:**
   - O primeiro a levar todas as 4 peças ao centro vence
   - Ao final, os jogadores são perguntados se desejam jogar novamente

### Controles
- `girar` - Rolar o dado
- `0`, `1`, `2`, `3` - Selecionar qual peça mover
- `skip` - Pular o turno (quando não consegue mover)
- `sim` ou `s` - Jogar novamente
- `nao` - Sair do jogo

