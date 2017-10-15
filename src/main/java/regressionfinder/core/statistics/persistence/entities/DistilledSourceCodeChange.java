package regressionfinder.core.statistics.persistence.entities;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("CODECHANGE")
public class DistilledSourceCodeChange extends DistilledChange {

	private Integer location;
	

	public Integer getLocation() {
		return location;
	}

	public void setLocation(Integer location) {
		this.location = location;
	}
}
