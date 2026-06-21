package com.king_sparkon_tracker.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "privileges", uniqueConstraints = @UniqueConstraint(name = "uk_privileges_name", columnNames = "name"))
public class Privilege {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, unique = true)
	private PrivilegeRole name;

	protected Privilege() {
	}

	public Privilege(PrivilegeRole name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public PrivilegeRole getName() {
		return name;
	}

	public void setName(PrivilegeRole name) {
		this.name = name;
	}
}
