import { useMemo } from 'react';
import { useSelector } from 'react-redux';
import { getAuthorizedRoutes } from '@/routes';

const useRoutePermission = () => {
  const userRole = useSelector((state: { user: IStoreUser }) => state.user.role);

  const authorizedRoutes = useMemo(() => {
    return getAuthorizedRoutes(userRole);
  }, [userRole]);

  return { authorizedRoutes };
};

export default useRoutePermission;
