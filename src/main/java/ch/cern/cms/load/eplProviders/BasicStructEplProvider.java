package ch.cern.cms.load.eplProviders;

import ch.cern.cms.load.EplProvider;
import ch.cern.cms.load.EventProcessor;

public class BasicStructEplProvider implements EplProvider {

	@Override
	public void registerStatements(EventProcessor ep) {
		ep.epl("create schema AbstractConclusion as (type String, title String, details String)");
		ep.epl("create variant schema ConclusionsStream as AbstractConclusion");
		ep.epl("create window Conclusions.win:keepall() as select * from ConclusionsStream");
		ep.epl("insert into Conclusions select * from ConclusionsStream");
	}

}
