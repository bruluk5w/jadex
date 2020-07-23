import {LitElement} from 'https://unpkg.com/lit-element@latest/lit-element.js?module';
import {html} from 'https://unpkg.com/lit-html@latest/lit-html.js?module';
import {css} from 'https://unpkg.com/lit-element@latest/lit-element.js?module';
import {BaseElement} from '/webcomponents/baseelement.js'

// Tag name 'jadex-starter'
class BpmnElement extends BaseElement {

	static get properties() 
	{ 
		return { cid: { type: String }};
	}
	
	attributeChangedCallback(name, oldVal, newVal) 
	{
	    console.log('attribute change: ', name, newVal, oldVal);
	    super.attributeChangedCallback(name, oldVal, newVal);
	    
		console.log("bpmn: "+this.cid);
	}
	
	init() {
		console.log("bpmn");
		
		this.model = null; // loaded model
		this.reversed = false;
		this.jadexservice = "jadex.tools.web.bpmn.IJCCBpmnService";
		
		let self = this;
		
		let styles = ["jadex/tools/web/bpmn/diagram-js.css",
					  "jadex/tools/web/bpmn/bpmn.css"];
		
		this.loadServiceFont('bpmn', 'jadex/tools/web/bpmn/bpmn.woff2').then((values) =>
		{
			console.log("BPMN load fonts ok");
			
			this.loadServiceStyles(styles).then((values) => 
			{
				console.log("BPMN load styles ok");
				
				let scripts = ["jadex/tools/web/bpmn/bpmnmodeler.js" ];
				this.loadServiceScripts(scripts).then((values) => 
				{
					console.log("BPMN load scripts ok");
					let celem = this.shadowRoot.getElementById('bpmnview');
					let bpmnmodeler = new BpmnJS({
			        	container: celem,
			        	keyboard: {
			          		bindTo: window
			        	}
					});
				});
			});
		});
		
		
		
		//const myElement = document.querySelector('my-element');
		this.addEventListener('jadex-model-selected', (e) => 
		{
			console.log(e)
			self.model = e.detail.model;
			self.requestUpdate();
		});
		
	}
	
	render() {
		return html`
		<div><div style="width:100%; height:100vh" id="bpmnview"/></div>
		`;
	}
}

customElements.define('jadex-bpmn', BpmnElement);
