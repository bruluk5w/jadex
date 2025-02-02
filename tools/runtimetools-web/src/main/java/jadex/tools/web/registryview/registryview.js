let { LitElement, html, css } = modLoad('lit-element');
let { BaseElement } = modLoad('base-element');
let { CidElement } = modLoad('cid-element');

// Tag name 'jadex-registry'
class RegistryViewElement extends CidElement 
{
	static get properties() 
	{
		var ret = {};
		if(super.properties!=null)
		{
			for (let key in super.properties)
				ret[key]=super.properties[key];
		}
		ret['global'] = {type: Boolean, attribute: false};
		return ret;
	}
	
	/*attributeChangedCallback(name, oldval, newval) 
	{
	    console.log('attribute change: ', name, newval, oldval);
	    super.attributeChangedCallback(name, oldval, newval);
	}*/
	
	init() 
	{
		console.log("relayview");
		
		this.subscriptions = {};
		//this.concom = false;
		//this.initready = false;
		this.global = true;
		
		this.app.lang.listeners.add(this);
		
		var res = "jadex/tools/web/registryview/conf.css";
		var ures = this.getMethodPrefix()+'&methodname=loadResource&args_0='+res+"&argtypes_0=java.lang.String";
		
		var self = this;
		
		//console.log("load style start");
		let ret = new Promise((resolve, reject) => {
			this.loadStyle(ures)
			.then(()=>
			{
				var res1 ="jadex/tools/web/registryview/st_styles.css";
				var ures1 = this.getMethodPrefix()+'&methodname=loadResource&args_0='+res1+"&argtypes_0=java.lang.String";
				var res2 ="jadex/tools/web/registryview/st_min.js";
				var ures2 = this.getMethodPrefix()+'&methodname=loadResource&args_0='+res2+"&argtypes_0=java.lang.String";
				
				//console.log("load datatables start");
				this.loadStyle(ures1)
				.then(()=>
				{
					//console.log("loaded datatables.js");
					
					this.loadScript(ures2)
					.then(()=>
					{
						//console.log("loaded datatables.css");
						resolve();
					})
					.catch((err)=>
					{
						console.log("b: "+err);
						reject(err);
					});
				})
				.catch((err)=>
				{
					console.log("a: "+err);
					reject(err);
				});
				
			})
			.catch((err)=>
			{
				console.log(err);
				reject(err);
			});
		});
		return ret;
	}
	
	postInit()
	{
		let self = this;
		
		let el = this.shadowRoot.getElementById("panel");
		el.addEventListener("click", this.collapseOnClick);
		
		let opts = {perPageSelect: [5,10,15,25,50,100,1000], perPage: 15};
		self.getSubscription("Services").table = new simpleDatatables.DataTable(self.shadowRoot.getElementById('tableservices'), opts);
		self.getSubscription("Platforms").table = new simpleDatatables.DataTable(self.shadowRoot.getElementById('tableplatforms'), opts);
		self.getSubscription("Queries").table = new simpleDatatables.DataTable(self.shadowRoot.getElementById('tablequeries'), opts);
		
		self.getSubscription("Services").table.insert({headings: ["","","","",""]});
		self.getSubscription("Platforms").table.insert({headings: ["","",""]})
		self.getSubscription("Queries").table.insert({headings: ["","",""]})
		
		//self.initready = true;
		self.subscribe();
	}
	
	/*connectedCallback()
	{
		super.connectedCallback();
		this.concom = true;	
	
		this.subscribe();
	}*/
	
	subscribe()
	{
		//console.log("initready: "+this.initready+", concom: "+this.concom);
		
		var self = this;
		
		//if(this.initready && this.concom)
		{
			console.log('reg subscribing');
			var wait = 5000;
			this.subscribeToX("Services", wait, 
				function(elem, event) 
				{ 
					return elem.name==event.service.serviceIdentifier.name && elem.providerId.name==event.service.serviceIdentifier.providerId.name;
				}, 
				function(table, event) 
				{
					var elem = event.service.serviceIdentifier;
					self.getSubscription("Services").elements.push(elem);
					if(self.global? elem.scope.toLowerCase()==="global": true)
						table.rows().add([elem.type, self.beautifyCid(elem.providerId.name), elem.scope.toLowerCase(), self.beautifyNetworks(elem.networkNames), elem.unrestricted && 'unrestricted' || 'restricted']);
				}
			);
			this.subscribeToX("Platforms", wait, 
				function(elem, event) 
				{ 
					elem.platform.name==event.platform.name && elem.protocol==event.protocol;
				}, 
				function(table, event) 
				{
					self.getSubscription("Platforms").elements.push(event);
					table.rows().add([event.platform.name, event.connected, event.protocol]);
				}
			);
			this.subscribeToX("Queries", wait, 
				function(elem, event) 
				{ 
					return elem.id==event.query.id;
				}, 
				function(table, event) 
				{
					var elem = event.query;
					self.getSubscription("Queries").elements.push(elem);
					if(self.global? elem.scope.value.toLowerCase()==="global": true)
						table.rows().add([elem.serviceType!=null? elem.serviceType.value: '', self.beautifyCid(elem.owner.name), elem.scope.value]);
				}
			);
		}
	}
	
	updated(props) 
	{
    	//console.log(props); 

		if(props.get("global")!=null)
		{
			this.syncTableDataX("Services", 
				(x) => this.global? x.scope.toLowerCase()==="global": true, 
				(x) => [x.type, this.beautifyCid(x.providerId.name), x.scope.toLowerCase(), this.beautifyNetworks(x.networkNames), x.unrestricted && 'unrestricted' || 'restricted']
			);
			
			this.syncTableDataX("Queries", 
				(x) => this.global? x.scope.value.toLowerCase()==="global": true, 
				(x) => [x.serviceType!=null? x.serviceType.value: '', this.beautifyCid(x.owner.name), x.scope.value]
			);
		}
  	}
	
	syncTableDataX(x, filter, convert)
	{
		var elems = this.getSubscription(x).elements.filter(filter);
		this.getSubscription(x).table.rows().remove("all");
		for(var e of elems)
		{
			this.getSubscription(x).table.rows().add(convert(e));
		}
	}
	
	disconnectedCallback()
	{
		super.disconnectedCallback();
		this.concom = false;	
		
		this.terminateSubscriptionX("Services");
		this.terminateSubscriptionX("Platforms");
		this.terminateSubscriptionX("Queries");
	}
	
	terminateSubscriptionX(x)
	{
		var tc = this.getSubscription(x).terminate;
		if(tc!=null)
		{
			//console.log("terminate: "+x);
			tc();
		}
	}
	
	
	getMethodPrefix() 
	{
		return 'webjcc/invokeServiceMethod?cid='+this.cid+'&servicetype=jadex.tools.web.registryview.IJCCRegistryViewService';
	}
	
	subscribeToX(x, interval, equals, add)
	{
		//console.log("subscribeTo"+x);
		
		this.terminateSubscriptionX(x);
		
		if(interval===undefined)
			interval = 5000;

		var self = this;
		self.getSubscription(x).terminate = jadex.getIntermediate(this.getMethodPrefix()+'&methodname=subscribeTo'+x+'&returntype=jadex.commons.future.ISubscriptionIntermediateFuture',
		function(response)
		{
			//console.log("service sub received: "+response.data);
			
			self.getSubscription(x).connected = true;
			self.updateX(x, response.data, equals, add);
		},
		function(response)
		{
			//console.log("Could not reach platform.");
			//console.log("Err: "+JSON.stringify(err));
			self.getSubscription(x).connected = true;
			self.getSubscription(x).elements = [];
			self.requestUpdate();
			
			setTimeout(function()
			{
				if(self.concom)
				{
					//console.log("Retrying platform connection...");
					self.subscribeToX(x, interval, equals, add);
				}
				else
				{
					//console.log("Subcribe terminated due to component disconnect: "+x);
				}
			}, interval);
		});
	}
	
	getSubscription(x)
	{
		if(this.subscriptions[x]==null)
			this.subscriptions[x] = {elements: []};
		return this.subscriptions[x];
	}
	
	updateX(x, event, equals, add)
	{
		var	found = false;
		
	//	alert("Service: "+JSON.stringify(service));
	
		var elms = this.getSubscription(x).elements;
		var table = this.getSubscription(x).table;
	
		for(var i=0; i<elms.length; i++)
		{
			found = equals(elms[i], event);
			if(found)
			{
				// 0: added, 1: removed, 2: changed
				if(event.type==1)	// removed
				{
					elems.splice(i,1);
					table.rows().remove(i);
				}
				else // added / changed
				{
					table.rows().remove(i);
					add(table, event);
				}
				break;
			}
		}
		
		if(!found)
			add(table, event);
			
		this.requestUpdate();
	}
	
	// helpers for string representation
	
	beautifyCid(cid)
	{
		var	cidparts = cid.split(/[@\.]+/);	// Split at '@' and '.', cf. https://stackoverflow.com/questions/650022/how-do-i-split-a-string-with-multiple-separators-in-javascript
		return cidparts.length>1 ? cidparts[cidparts.length-1] +" ("+cid+")" : cid;
	}
	
	beautifyNetworks(networks)
	{
		var nets = null;
		if(networks!=null)
		{
			networks.forEach(function(network)
			{
				if("___GLOBAL___".localeCompare(network)!=0)
				{
					if(nets==null)
					{
						nets = network;
					}
					else
					{
						nets += ", "+network
					}
				}
			});
		}
		return nets==null ? "" : nets;
	}
	
	// helpers for bootstrap panel
	
	// Bootstrap collapse not working in shadow dom :-(
	// This method allows for opening closing collapse elements
	collapseOnClick(e)
	{
    	var target = e.target.getAttribute("data-target")
		if(target==null)
			target = e.target.getAttribute("href");
		if(target!=null)
		{
			if(target.startsWith("#"))
				target = target.substring(1);
			//var el = $(target, this.parentNode);
			var el = this.parentNode.getElementById(target);
			if(el!=null)
			{
    			//el.collapse('toggle');
				if(el.classList.contains("show"))
					el.classList.remove("show");
				else
					el.classList.add("show");
			}
		}
    }
	
	/*getFilteredElements()
	{
		var ret = this.getSubscription("Services").elements.filter(x => this.global? x.scope.toLowerCase()==="global": true)
		return ret;
	}*/
	
	requestUpdate()
	{
		// Update table headers according to language 
		// not possible in HTML as texts are read only once
		
		if(this.getSubscription("Services")!=null && this.getSubscription("Services").table!=null)
		{
			this.getSubscription("Services").table.headings[0].innerText=this.app.lang.t('Service Type');
			this.getSubscription("Services").table.headings[1].innerText=this.app.lang.t('Provided By');
			this.getSubscription("Services").table.headings[2].innerText=this.app.lang.t('Publication Scope');
			this.getSubscription("Services").table.headings[3].innerText=this.app.lang.t('Networks');
			this.getSubscription("Services").table.headings[4].innerText=this.app.lang.t('Security');
			
			this.getSubscription("Platforms").table.headings[1].innerText=this.app.lang.t('Connected');
			this.getSubscription("Platforms").table.headings[2].innerText=this.app.lang.t('Protocol');
			
			this.getSubscription("Queries").table.headings[0].innerText=this.app.lang.t('Service Type');
			this.getSubscription("Queries").table.headings[1].innerText=this.app.lang.t('Query Owner');
			this.getSubscription("Queries").table.headings[2].innerText=this.app.lang.t('Search Scope');
		}
		
		return super.requestUpdate();
	}
	
	asyncRender() 
	{
		return html`
		<div id="tab"></div>
		
		<!--<lit-datatable .data="${this.services}" .conf="${[{ property: 'type', header: 'Service Type', hidden: false }, { property: 'providerId.name', header: 'Provided By', hidden: false }]}"></lit-datatable>-->
		
		<div id="panel" class="container-fluid m-0 p-0">
			<div>
    			<input type="checkbox" id="global" .checked="${this.global}" @click="${(e) => {this.global = e.target.checked;}}">
				<label for="global">${this.app.lang.t('Show only global services and queries')}</label>
  			</div>
			<div id="accordion">
				<div class="card m-0">
					<div class="card-header">
    					<h4 class="card-link" data-toggle="collapse" href="#collapseOne">${this.app.lang.t('Services')}</h4>
					</div>
					<div id="collapseOne" class="collapse show" data-parent="#accordion">
						<div class="card-body">
							<table id="tableservices" class="${this.getSubscription("Services")?'': 'down'}">
							</table>
						</div>
    				</div>
  				</div>
		
				<div class="card m-0">
					<div class="card-header">
						<h4 class="collapsed card-link" data-toggle="collapse" href="#collapseTwo">${this.app.lang.t('Platforms')}</h4>
					</div>
					<div id="collapseTwo" class="collapse" data-parent="#accordion">
						<div class="card-body">
							<table id="tableplatforms" class="${this.getSubscription("Platforms").connected?'': 'down'}">
							</table>
						</div>
					</div>
				</div>
				
				<div class="card m-0">
					<div class="card-header">
						<h4 class="collapsed card-link" data-toggle="collapse" href="#collapseThree">Queries</h4>
					</div>
					<div id="collapseThree" class="collapse" data-parent="#accordion">
						<div class="card-body">
							<table id="tablequeries" class="${this.getSubscription("Queries")?'': 'down'}">
							</table>
						</div>
					</div>
				</div>
			</div>
		</div>
		`;
	}
}

if(customElements.get('jadex-registry') === undefined)
	customElements.define('jadex-registry', RegistryViewElement);
