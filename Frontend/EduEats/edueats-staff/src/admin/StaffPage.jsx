import UsersPage from "./UsersPage";

// CHANGED: same table as End-Users (search, status filter, suspend,
// last login), plus "New Staff Account".
function StaffPage({ staff = [], loading = false, onViewUser, onUserChanged, onAdd }) {
  return (
    <UsersPage
      users={staff}
      loading={loading}
      onViewUser={onViewUser}
      onUserChanged={onUserChanged}
      breadcrumb="EDUEATS / OPERATIONAL STAFF"
      title="Staff Accounts"
      noun="staff members"
      showCustomerCode={false}
      addLabel="New Staff Account"
      onAdd={onAdd}
    />
  );
}

export default StaffPage;
