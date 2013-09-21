package com.pmease.gitop.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.pmease.commons.hibernate.AbstractEntity;
import com.pmease.gitop.core.gatekeeper.GateKeeper;
import com.pmease.gitop.core.permission.object.ProtectedObject;
import com.pmease.gitop.core.permission.object.UserBelonging;
import com.pmease.gitop.core.permission.operation.RepositoryOperation;

@Entity
@Table(uniqueConstraints={
		@UniqueConstraint(columnNames={"owner", "name"})
})
@SuppressWarnings("serial")
public class Repository extends AbstractEntity implements UserBelonging {
	
	@ManyToOne
	@JoinColumn(nullable=false)
	private User owner;

	@Column(nullable=false)
	private String name;
	
	private String description;

	private GateKeeper gateKeeper;

	@OneToMany(mappedBy="repository")
	private Collection<RepositoryAuthorization> authorizations = new ArrayList<RepositoryAuthorization>();

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public GateKeeper getGateKeeper() {
		if (gateKeeper != null)
			gateKeeper = (GateKeeper) gateKeeper.trim(this);
		return gateKeeper;
	}

	public void setGateKeeper(GateKeeper gateKeeper) {
		this.gateKeeper = gateKeeper;
	}

	@Override
	public User getUser() {
		return getOwner();
	}

	public Collection<RepositoryAuthorization> getAuthorizations() {
		return authorizations;
	}

	public void setAuthorizations(Collection<RepositoryAuthorization> authorizations) {
		this.authorizations = authorizations;
	}

	@Override
	public boolean has(ProtectedObject object) {
		if (object instanceof Repository) {
			Repository repository = (Repository) object;
			return repository.getId().equals(getId());
		} else {
			return false;
		}
	}

	public Collection<User> findAuthorizedUsers(RepositoryOperation operation) {
		Map<Long, Boolean> authorizationMap = new HashMap<Long, Boolean>();
		for (RepositoryAuthorization authorization: getAuthorizations()) {
			authorizationMap.put(authorization.getTeam().getId(), authorization.getAuthorizedOperation().can(operation));
		}
		
		Collection<Team> teams = new HashSet<Team>();
		for (Team team: getOwner().getTeams()) {
			Boolean authorized = authorizationMap.get(team.getId());
			if (authorized != null) {
				if (authorized)
					teams.add(team);
				else
					continue;
			} else {
				if (team.getAuthorizedOperation().can(operation))
					teams.add(team);
			}
		}
		
		Collection<User> users = new HashSet<User>();
		for (Team team: teams) {
			for (TeamMembership membership: team.getMemberships())
				users.add(membership.getUser());
		}
		
		return users;
	}
	
}
