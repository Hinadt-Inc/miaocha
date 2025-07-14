import type { TypedUseSelectorHook } from 'react-redux';
import { useDispatch, useSelector } from 'react-redux';
import type { AppDispatch, RootState } from '@/store/store';

// 建议在项目的各个地方使用这个自定义hook（useAppDispatch、useAppSelector），而不是直接使用 useDispatch 和 useSelector。
export const useAppDispatch: () => AppDispatch = useDispatch;
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
