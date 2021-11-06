

# CACHE HTTP

Normatizado na [RFC 7232](https://datatracker.ietf.org/doc/html/rfc7232), reduz o tráfico entre server e client.   
***IMPORTANTE*:**  essas configurações só serão úteis em navegadores. Outros tipos de aplicação não se beneficiarão desses recursos;

**Ferramentas que iremos usar :**

1. *Talend API Tester - Free Edition* -  client rest para usar dentro do navegador 
   1. Buscar no google `*Talend API Tester - Free Edition*`   
   2. Clicar para adicionar no Chrome
2. *WireShark* - para inspecionar o corpo das requisições e responses
   1.  Baixar e instalar  https://www.wireshark.org/



## Para que serve o Cache de HTTP

O **cache de http serve** basicamente **para reduzir o trafego do payload** da requisição entre servidor / cliente.
Uma coisa interessante é que, uma vez que a informação esteja no cache local, mesmo sem internet o navegador vai entregar o payload normalmente;

**Existem dois tipos de cache de http que podem ser usados**:
**Cache local** - esse cache fica sempre no navegador do cliente - exemplificar esse tipo é o **foco deste projeto**
**Cache compartilhado** - fica em um servidor de proxy reverso, por exemplo o NGINX, dessa forma vários clientes se beneficiam do mesmo cache.



#### Cache Local

O navegador possui um **cache local** e quando você faz uma requisição ele antes de ir ao server ele consulta esse cache local. isso tudo **de forma transparente ao desenvolvedor do front**

Esse cache local possui um maxAge(tempo máximo que o payload fica no cache local) que deve ser configurado no servidor dessa forma o cache local pode ter dois estados :
	**fresh** : ainda dentro do maxAge
	**stale** : maxAge expirado



###### Definindo o maxAge :

* Adiciona no header Cache-Control: max-age=xx onde o xx é em segundos 
```
    @GetMapping("/customer/")
    public ResponseEntity<List<Customer>> listAll() {
        log.info("Request made "+LocalDateTime.now());
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS)) // aqui está a magica 
            .body(customerService.listAll());
    }
    
```



#### Etags - Entity Tags   - uma forma mais inteligente que controlar apenas o maxAge

* Ao fazer a requisição o servidor irá enviar junto com o payload um **eTag** (hash das informações do payload)  

* O payload, junto com o eTag, vai para o cache local com o devido maxAge

* Se feito nova requisição após a expiração do maxAge, o  **próprio navegador** adiciona no cabeçalho desta requisição o header **If-None-Match**: "eTag"  

* Se não houver modificações no payload a resposta do servidor,o eTag ainda será o mesmo, e o servidor responderá com o **http-status 304 (not modified)**, tornando o cache local fresh novamente. 

* Se houver modificações no payload a resposta do servidor será o novo payload junto com nova eTag    

  

  **IMPORTANTE:** 

  1. A requisição será feita normalmente, o servidor irá processar a requisição normalmente, apenas o payload de retorno não será enviado quando ainda for o mesmo eTag. Neste caso o payload será pego do cache local  do navegador.
  2. Isso só funciona de forma automágica se o cliente for um navegador, 
  3. Como proceder caso o client não seja um navegador
     1. Criar um BD sqLite com a URL da requisição, com a eTag atual e com o resultado do payload
     2. Toda vez que fazer o GET passar no header da requisição *If-None-Match:<eTag-atual>*
     3. Isso fará com que caso esteja ainda no cache a aplicação tenha um response com o http-status 304 (not modified)
     4. Dessa forma, se for 304, pega do cache e serve a aplicação, caso contrario, armazena o novo payload no sqLite junto com a nova eTag;



#### Configurando o projeto para uso do eTag de forma automática - Método ***Shallow eTag*** (eTag Raso)   

Para usar essa forma basta definir o seguinte bean em uma classe de configuração: (@Configuration)
```
        @Bean
        public Filter shallowEtagHeaderFilter() {
            return new ShallowEtagHeaderFilter();
        }
```

##### Para visualizar funcionando :
* Entrar no **wireshark**

* escolher "Adapter for loopback trafic capture"

* filtrar http

* Entrar no **Talend ApI tester** - fazer as requisições e acompanhar o resultado de quando expira o maxAge

* Você verá que o payload não será mais trafegado após a expiração do maxAge;

* Na classe [CustomerRepository] (src/main/java/com/example/cachehttp/respositories/CustomerRepository.java) foi criada uma regra para alterar o primeiro registro a cada 10 segundos, isso fará com que o cache local seja substituído 

  ```
  // se você desejar ver ele pegando somente do cache comente o trecho abaixo :
  		var name  = "luck";
          int thisSecond = LocalDateTime.now().getSecond();
          
          /* Just to simulate changes on record every ten seconds */
          if(thisSecond < 10) {
              name = name + " 1" ;
  
          } else if(thisSecond >= 10 && thisSecond < 20) {
              name = name + " 2" ;
  
          } else if(thisSecond >= 20 && thisSecond < 30) {
              name = name + " 3" ;
  
          } else if(thisSecond >= 30 && thisSecond < 40) {
              name = name + " 4" ;
  
          } else if(thisSecond >= 40 && thisSecond < 50) {
              name = name + " 5" ;
  
          } else if(thisSecond >= 50 && thisSecond <= 60) {
              name = name + " 6" ;
  
          }
  
  ```

  

#### Outras diretivas:

* .*cachePrivate*() - armazenamento apenas em cache local, não armazenando em servidores de proxy reverso.
```
    // USO:
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePrivate())
            .body(customerService.listAll());
```

* .*cachePublic*() - ***(DEFAULT)*** - armazena no cache local e no cache do proxy reverso 
```
    // USO:
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePublic())
            .body(customerService.listAll());
```

* *noCache*() - faz o cache normalmente, mas sempre valida a eTag  **(não confundir com .*noStore()*)**
```
       // USO:
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(customerService.listAll());
```

* *noStore*() - não faz cache  
```
       // USO:
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noStore())
            .body(customerService.listAll());
```



#### Como o desenvolvedor do front faz para a requisição não pegar do cache local:

Caso o desenvolvedor front não queira pegar informações do cache local, basta ele adicionar no header da requisição:  
_Cache-Control:no-cache_  
**Não confunda** com o response do server, esse é do request, feito pela aplicação cliente.




#### Metodo **Deep eTag** (eTag profunda)   
Em resumo seria criar o eTag na mão e adicionar ao cabeçalho da requisição, usando para validar o "*If-None-Match*" o método 
_request.checkNotModified(eTag)_.   
Exemplo:   

```
public ResponseEntity<List<FormaPagamentoModel>> listar(ServletWebRequest request) {
		ShallowEtagHeaderFilter.disableContentCaching(request.getRequest()); // desabilita o 'shallow Filter'
		
		String eTag = "0";
		
        // logica para geração do eTag
		OffsetDateTime dataUltimaAtualizacao = formaPagamentoRepository.getDataUltimaAtualizacao();
		
		if (dataUltimaAtualizacao != null) {
			eTag = String.valueOf(dataUltimaAtualizacao.toEpochSecond());
		}
		
        /**
         verifica o If-None-Match se não tiver tido alterações na eTag - retorna null pois o resultado 
         já será pego pelo navegador do cache local do browser;
        **/
		if (request.checkNotModified(eTag)) { 
			return null;
		}
		
		List<FormaPagamento> todasFormasPagamentos = formaPagamentoRepository.findAll();
		
		List<FormaPagamentoModel> formasPagamentosModel = formaPagamentoModelAssembler
				.toCollectionModel(todasFormasPagamentos);
		
		return ResponseEntity.ok()
				.cacheControl(CacheControl.maxAge(10, TimeUnit.SECONDS).cachePublic())
				.eTag(eTag)
				.body(formasPagamentosModel);
	}
```


### Cache de Http com eTag em conjunto com Redis:

A principal vantagem é que, mesmo que o TTL do redis tenha expirado, ainda, corre o riso do payload não ter de trafegar pela rede pois o eTag pode não ter mudado.



Espero ter ajudado 

​	by Luck :smile:

