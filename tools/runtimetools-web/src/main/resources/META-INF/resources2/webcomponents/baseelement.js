import {LitElement} from 'https://unpkg.com/lit-element@latest/lit-element.js?module';
import {html} from 'https://unpkg.com/lit-html@latest/lit-html.js?module';
import {css} from 'https://unpkg.com/lit-element@latest/lit-element.js?module';

export class BaseElement extends LitElement 
{
	static loaded = {};
	
	cid = null;
	jadexservice = null;
	
	static get properties() 
	{ 
		return { cid: { type: String }};
	}
	
	attributeChangedCallback(name, oldVal, newVal) 
	{
	    console.log('attribute change: ', name, newVal, oldVal);
	    super.attributeChangedCallback(name, oldVal, newVal);
	    
	    if (name === 'cid' && typeof this.init === 'function')
	    	this.init();
	    
		console.log("starter: "+this.cid);
	}
	
	constructor() 
	{
		super();
		this.loadStyle("/css/style.css")
			.then(()=>{console.log("loaded jadex css")})
			.catch((e)=>{console.log("error loading jadex css: "+e)});
		this.loadStyle("https://stackpath.bootstrapcdn.com/bootstrap/4.5.0/css/bootstrap.min.css")
			.then(()=>{console.log("loaded bootstrap css")})
			.catch((e)=>{console.log("error loading boostrap css: "+e)});
		this.loadScript("libs/jquery_3.4.1/jquery.js")
			.then(()=>{console.log("loaded jquery")})
			.catch((e)=>{console.log("error loading jquery: "+e)});
		this.loadScript("https://stackpath.bootstrapcdn.com/bootstrap/4.5.0/js/bootstrap.bundle.min.js")
			.then(()=>{console.log("loaded bootstrap")})
			.catch((e)=>{console.log("error loading bootstrap: "+e)});
	}
	
	loadStyle(url)
	{
		var self = this;
		
		return new Promise(function(resolve, reject) 
		{
			axios.get(url).then(function(resp)
			{
				var css = resp.data;    
				//console.log(css);
				var sheet = new CSSStyleSheet();
				sheet.replaceSync(css);
				self.shadowRoot.adoptedStyleSheets = self.shadowRoot.adoptedStyleSheets.concat(sheet);
				resolve(sheet);
			})
			.catch(function(err)
			{
				reject(err);
			});
		});
	}
	
	loadScript(url)
    {
		if(window.loadedScript==null)
		{
			window.calls = {};
			window.loadedScript = function(url)
			{
				var callback = window.calls[url];
				if(callback!=null)
				{
					delete window.calls[url];
					callback();
				}
				else
				{
					console.log("Callback not found for: "+url);
				}
			}
		}
		
   		return new Promise(function(resolve, reject) 
		{
   			if(BaseElement.loaded[url]!=null)
   	    	{
   	    		console.log("already loaded script: "+url);
   	    		resolve();
   	    	}
   	    	else
   	    	{
   	    		BaseElement.loaded[url] = url;
   	    		
   				console.log("loading script content start: "+url);
   	    		axios.get(url).then(function(resp)
   				{
   					console.log("loaded script content end: "+url);//+" "+resp.data);
   					
   					// directly loading via a script src attributes has the
   					// disadvantage that the type is checked from the response
   					// throwing check errors :-(
   							
   					var js = resp.data;    				
   		            var script = document.createElement('script');
   		            script.type = "module";
   		            //script.textContent = js+"\n console.log('LOOADED: "+url+"'); window.loadedScript('"+url+"')";
   		            script.textContent = js+"\n window.loadedScript('"+url+"')";
   		            //script.innerHTML = js;
   		            //script.async = false;
   		            //script.onload = function() {console.log("LOADED SCRIPT: "+url);};
   		            window.calls[url] = resolve;
   		            document.getElementsByTagName("head")[0].appendChild(script);
   		            // https://stackoverflow.com/questions/40663150/script-injected-with-innerhtml-doesnt-trigger-onload-and-onerror
   		            // append child returns BEFORE script is executed :-( In contrast to text

   		            //console.log('APPENDED: '+url);
   		            //setInterval(function(){ resolve(); }, 1000);
   				})
   				.catch(function(err)
   				{
   					console.log("Error loading script: "+url);
   					reject();
   				});
   	    	}
		});	
    }
	
	loadServiceStyle(stylepath)
    {
		return this.loadStyle(this.getResourceUrl(stylepath));
    }
	
	loadServiceStyles(stylepaths)
    {
		let stylepromises = [];
		let self = this;
		stylepaths.forEach(function(stylepath) {
			stylepromises.push(self.loadServiceStyle(stylepath));
		})
		
		return Promise.all(stylepromises);
    }
	
	loadServiceScript(scriptpath)
    {
		return this.loadScript(this.getResourceUrl(scriptpath));
    }
	
	loadServiceScripts(scriptpaths)
    {
		let scriptpromises = [];
		let self = this;
		scriptpaths.forEach(function(scriptpath) {
			scriptpromises.push(self.loadServiceScript(scriptpath));
		})
		
		return Promise.all(scriptpromises);
    }
	
	loadServiceFont(fontfamily, fontpath)
	{
		let self = this;
		console.log("loadServiceFont");
		return new Promise(function(resolve, reject) 
		{
			console.log("loadServiceFont Promise exec");
			let font = new FontFace(fontfamily, 'url(' + self.getResourceUrl(fontpath) + ')');
			font.load().then(function(fontface)
			{
			    document.fonts.add(fontface)
			    resolve(fontface);
			}).catch(function(err)
			{
				reject(err);
			});
		});
	}
	
	getResourceUrl(respath)
	{
		let prefix = 'webjcc/invokeServiceMethod?cid='+this.cid+'&servicetype='+this.jadexservice;
		let url = prefix+'&methodname=loadResource&args_0='+respath+"&argtypes_0=java.lang.String";
		return url;
	}
	
	switchLanguage() 
	{
	    language.switchLanguage(); 
	    this.requestUpdate(); // needs manual update as language.lang is not mapped to an attribute 
	}
}

//customElements.define('jadex-base', StarterElement);
