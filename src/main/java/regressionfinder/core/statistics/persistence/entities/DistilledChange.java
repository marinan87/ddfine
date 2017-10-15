package regressionfinder.core.statistics.persistence.entities;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("CHANGE")
@Table(
		name = "DISTILLED_CHANGE",
		uniqueConstraints = @UniqueConstraint(columnNames = {"EXECUTION_ID", "CHUNK_NUMBER"})
)
public class DistilledChange {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
	
	@Column(name = "CHUNK_NUMBER", nullable = false)
	private Integer chunkNumber;
	
	private String changeType;
	
	private String changePath;

	
	public Long getId() {
		return id;
	}
	
	public Integer getChunkNumber() {
		return chunkNumber;
	}

	public void setChunkNumber(Integer chunkNumber) {
		this.chunkNumber = chunkNumber;
	}

	public String getChangeType() {
		return changeType;
	}

	public void setChangeType(String changeType) {
		this.changeType = changeType;
	}

	public String getChangePath() {
		return changePath;
	}

	public void setChangePath(String changePath) {
		this.changePath = changePath;
	}
}
