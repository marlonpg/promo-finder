package dev.gamba.promofinder.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/** A shop the user trusts, e.g. "Amazon BR" / "amazon.com.br". */
@Entity
public class Store {

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false)
	private String name;

	/** Domain used to restrict searches and validate offer URLs, e.g. "magazineluiza.com.br". */
	@Column(nullable = false)
	private String domain;

	private boolean enabled = true;

	/** Soft delete, so the UI can offer "Undo". */
	private Instant deletedAt;

	public Long getId() { return id; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getDomain() { return domain; }
	public void setDomain(String domain) { this.domain = domain; }
	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public Instant getDeletedAt() { return deletedAt; }
	public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
