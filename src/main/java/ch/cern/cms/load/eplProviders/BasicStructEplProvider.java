package ch.cern.cms.load.eplProviders;

import ch.cern.cms.esper.StatementsLifecycleManager;
import ch.cern.cms.load.EplProvider;
import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.StatementsToggler;

public class BasicStructEplProvider implements EplProvider {

	@Override
	public void registerStatements(EventProcessor ep) {

		ep.epl("create window SuspendedStatements.win:keepall() as (name String)");
		ep.epl("select * from SuspendedStatements").setSubscriber(new StatementsLifecycleManager.SubscriberSuspender());
		ep.epl("select rstream * from SuspendedStatements").setSubscriber(new StatementsLifecycleManager.SubscriberResumer());

		ep.epl("create objectarray schema DebugMsg as (message String)");
		
		ep.epl("create schema AbstractConclusion as (type String, title String, details String)");
		ep.epl("create variant schema ConclusionsStream as AbstractConclusion");
		ep.epl("create window Conclusions.win:keepall() as select * from ConclusionsStream");
		ep.epl("insert into Conclusions select * from ConclusionsStream");
	}

}
