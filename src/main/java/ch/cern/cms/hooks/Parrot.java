package ch.cern.cms.hooks;

import com.espertech.esper.client.annotation.Hook;
import com.espertech.esper.client.annotation.HookType;

@Hook(hook = "Parrot", type = HookType.SQLCOL)
public class Parrot {
	public static final void print(Object o) {
		System.out.println(o != null ? o.toString() : "N u LLL!");
	}
}
