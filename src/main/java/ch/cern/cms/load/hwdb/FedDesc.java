package ch.cern.cms.load.hwdb;

public class FedDesc {
	public final Integer link;
	public final Integer slot;
	public final String context;
	public final CmsHw hw;

	public FedDesc(Integer link, Integer slot, String context, CmsHw hw) {
		super();
		this.link = link;
		this.slot = slot;
		this.context = context;
		this.hw = hw;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((context == null) ? 0 : context.hashCode());
		result = prime * result + ((hw == null) ? 0 : hw.hashCode());
		result = prime * result + ((link == null) ? 0 : link.hashCode());
		result = prime * result + ((slot == null) ? 0 : slot.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FedDesc other = (FedDesc) obj;
		if (context == null) {
			if (other.context != null)
				return false;
		} else if (!context.equals(other.context))
			return false;
		if (hw != other.hw)
			return false;
		if (link == null) {
			if (other.link != null)
				return false;
		} else if (!link.equals(other.link))
			return false;
		if (slot == null) {
			if (other.slot != null)
				return false;
		} else if (!slot.equals(other.slot))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FedDesc [link=" + link + ", slot=" + slot + ", context=" + context + ", hw=" + hw + "]";
	}
}
